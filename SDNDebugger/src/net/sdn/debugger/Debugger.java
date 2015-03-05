package net.sdn.debugger;

/**
 * @author Da Yu, Yiming Li, Tim Nelson
 */
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.gson.Gson;

import net.sdn.event.Event;
import net.sdn.event.NetworkEvent;
import net.sdn.event.packet.PacketType;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import rx.Notification;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

// Don't need this here. Scala provides JavaConversions object to convert from
// Java Observable to Scala Observable (where we can use lambdas, etc.)
//import rx.lang.scala.*;

class ErrorEvent extends NetworkEvent {
	Throwable exn;

	public ErrorEvent(Throwable exn) {
		this.exn = exn;
	}
}

// Not abstract anymore
public class Debugger implements Runnable {
	public void run() {
		createServer().startAndWait();
	}

	// Start with an empty stream that doesn't terminate...
	public Observable<Event> events = Observable.never();

	protected List<NetworkEvent> expectedEvents = new LinkedList<>();
	protected List<NetworkEvent> notExpectedEvents = new LinkedList<>();
	protected HashSet<PacketType> interestedEvents = new HashSet<PacketType>();

	private final int port = 8200;
	private final long EXPIRE_TIME = 1000 * 1000000; // nano seconds
	private String partialLine = "";

	public Debugger() {
	}

	public static Action1<Event> func_printevent = new Action1<Event>() {
		@Override
		public void call(Event e) {
			System.out.println("Event: " + e.toString());
		}
	};

	private void timer(NetworkEvent e) {
		// clean expired events in notExpected and raise error in expectedEvent
		Iterator<NetworkEvent> it = this.notExpectedEvents.iterator();
		while (it.hasNext()) {
			// remove expired rules
			NetworkEvent temp = it.next();
			if (e.timeStamp - temp.timeStamp >= EXPIRE_TIME) {
				System.out.println("Not Expected Event Expired:");
				System.out.println(temp);
				it.remove();
			} else
				break;
		}

		it = this.expectedEvents.iterator();
		while (it.hasNext()) {
			NetworkEvent ev = it.next();
			if (e.timeStamp - ev.timeStamp >= EXPIRE_TIME) {
				System.err.println("Expected Event but Not Happened:");
				System.err.println(ev);
				it.remove();
			} else
				break;
		}
	}

	// Extend current partial line by <msg> and extract what full lines have
	// been created.
	String[] getFullMessages(String msg) {
		partialLine += msg;
		String temp[] = partialLine.split("\n");
		if (partialLine.endsWith("\n")) {
			// full message line
			partialLine = "";
			return temp;
		} else {
			// part message line
			partialLine = temp[temp.length - 1];
			return java.util.Arrays.copyOf(temp, temp.length - 1);
		}
	}

	// Is this event an OpenFlow echo request or reply?
	// TODO: why all of these null checks? Should be a method in the class for
	// this.
	boolean isOFEcho(NetworkEvent eve) {
		return eve.pkt.eth != null
				&& eve.pkt.eth.ip != null
				&& eve.pkt.eth.ip.tcp != null
				&& eve.pkt.eth.ip.tcp.of_packet != null
				&& (eve.pkt.eth.ip.tcp.of_packet.type
						.equalsIgnoreCase("echo_reply") || eve.pkt.eth.ip.tcp.of_packet.type
						.equalsIgnoreCase("echo_request"));
	}

	private Observable<Event> buildNewStream(
			ObservableConnection<String, String> connection) {
		return connection.getInput().flatMap(
		// flatMap over the stream of string chunks to create event stream
		// This func turns every string into a stream of events (possibly empty)
				new Func1<String, Observable<Event>>() {
					@Override
					public Observable<Event> call(String msg) {
						// Add the new string and see if we get any full
						// messages
						String[] fullMessages = getFullMessages(msg);
						List<Event> result = new ArrayList<Event>();
						for (String fullMessage : fullMessages) {
							// get event; deserialize
							// TODO: why re-create the Gson object for every
							// event?
							Gson gson = new Gson();
							NetworkEvent eve = gson.fromJson(fullMessage,
									NetworkEvent.class);
							// check expired rules and gc
							synchronized (this) {
								timer(eve);
								// if (isOFEcho(eve))
								// return Observable.empty();
								result.add(eve);
							}
						}

						// Return a stream of 0..n events. flatMap will combine
						// the streams in order.
						// System.out.println("Debug: adding event(s): "+result.toString());
						return Observable.from(result); // .just would try to
														// create an
														// Observable<Set<Event>>
					}
				}) // end flatMap to construct stream of full events
					// "onErrorReturn will instead emit a specified item and invoke the observer’s onCompleted method."
				.onErrorReturn(new Func1<Throwable, NetworkEvent>() {
					@Override
					public NetworkEvent call(Throwable exn) {
						System.out.println(" --> Error/Exception thrown in stream. Returning an ErrorEvent and stopping.");
						System.out.println(" --> "+exn.toString());
						exn.printStackTrace();
						System.out.println("\n\n");
						return new ErrorEvent(exn); // include error context in
													// stream
					}
				});
	}

	protected RxServer<String, String> createServer() {
		RxServer<String, String> server = RxNetty.createTcpServer(port,
				PipelineConfigurators.textOnlyConfigurator(),
				new ConnectionHandler<String, String>() {
					@Override
					// "Invoked whenever a new connection is established." Must
					// return Observable<Void>
					public Observable<Void> handle(
							final ObservableConnection<String, String> connection) {
						System.out
								.println("\nA monitor connected to the debugger...\n");

						/*
						 * val o = Observable.just(1,2,3,4) o.subscribe(n =>
						 * println("n = " + n)) o.subscribe(n => println("n = "
						 * + n)) // prints the sequence twice (this is
						 * "cold observable" behavior) subscribe returns a
						 * subscription object, which at this point is
						 * unsubscribed.
						 * 
						 * merging will complete both streams unless one returns
						 * an error (not same as complete!)
						 */

						// Build the stream of events from this new monitor
						Observable<Event> newStream = buildNewStream(connection);
						// May have multiple streams coming from multiple
						// connections, so merge them.
						events = Observable.merge(events, newStream);

						// keep going while not an error
						return connection.getInput()
						// return newStream // *** It was not safe to
						// use the above ^. Needed to use newStream
						// here. Why?
								.flatMap(
										new Func1<String, Observable<Notification<Void>>>() {
											@Override
											public Observable<Notification<Void>> call(
													String str) {
												// System.out.println("debug: flatmap: "+e.toString());
												return Observable.empty(); // normally
																			// would
																			// call
																			// materialize()
																			// here
																			// to
																			// get
																			// proper
																			// return
																			// type
											}
										})

								// A Notification is a message _to_ an Observer
								// *** NOTE *** The Javadoc says "observable" in
								// places, but this is wrong!
								// can be OnError, OnCompleted, etc.

								// Even though above will flatten to the empty
								// stream, this can still be called if an error
								// occurs...
								.takeWhile(
										new Func1<Notification<Void>, Boolean>() {
											@Override
											public Boolean call(
													Notification<Void> notification) {
												// System.out.println("debug: takewhile predicate (expect not to see): "+!notification.isOnError());
												return !notification
														.isOnError();
											} // once an error, print this
												// message
										}).finallyDo(new Action0() {
									@Override
									public void call() {
										// This happens when we ctrl-C out of
										// the monitor window
										System.out
												.println(" --> Error in connection; closing monitor handler and stream...");
									}

								}).map(new Func1<Notification<Void>, Void>() {
									@Override
									public Void call(
											Notification<Void> notification) {
										// System.out.println("debug: null (expect not to see)");
										return null; // need to return an
														// Observable<Void>, so
														// map into Void (which
														// is uninstantiable)
									}
								});

					} // end handle
				});

		// System.out.println("Monitor handler started. Waiting for connections.\n");
		return server;
	}

	protected void addExpectedEvents(NetworkEvent eve) {
		System.out.println("Adding Expected Event:");
		System.out.println(eve);
		for (int i = 0; i < expectedEvents.size(); i++) {
			if (expectedEvents.get(i).priority <= eve.priority) {
				expectedEvents.add(i, eve);
				return;
			}
		}
		expectedEvents.add(eve);
	}

	protected void addNotExpectedEvents(NetworkEvent eve) {
		System.out.println("Adding Not Expected Event:");
		System.out.println(eve);
		for (int i = 0; i < notExpectedEvents.size(); i++) {
			if (notExpectedEvents.get(i).priority <= eve.priority) {
				notExpectedEvents.add(i, eve);
				return;
			}
		}
		notExpectedEvents.add(eve);
	}

	protected void addInterestedEvents(PacketType t) {
		interestedEvents.add(t);
	}

	// always allow heartbeat for rule expriations
	private boolean isInterestedEvent(NetworkEvent e) {
		// System.out.println(e);
		if ((interestedEvents.contains(PacketType.ARP) && e.pkt.eth.arp != null)
				|| (interestedEvents.contains(PacketType.IP) && e.pkt.eth.ip != null)
				|| (interestedEvents.contains(PacketType.ICMP)
						&& e.pkt.eth.ip != null && e.pkt.eth.ip.icmp != null)
				|| (interestedEvents.contains(PacketType.TCP)
						&& e.pkt.eth.ip != null && e.pkt.eth.ip.tcp != null && e.pkt.eth.ip.tcp.of_packet == null)
				|| (interestedEvents.contains(PacketType.UDP)
						&& e.pkt.eth.ip != null && e.pkt.eth.ip.udp != null)
				|| (interestedEvents.contains(PacketType.OF)
						&& e.pkt.eth.ip != null && e.pkt.eth.ip.tcp != null && e.pkt.eth.ip.tcp.of_packet != null)
				|| (e.pkt.eth.ip != null && e.pkt.eth.ip.tcp != null
						&& e.pkt.eth.ip.tcp.of_packet != null && (e.pkt.eth.ip.tcp.of_packet.type
						.equals("echo_reply") || e.pkt.eth.ip.tcp.of_packet.type
						.equals("echo_request")))) {
			return true;
		}
		return false;
	}

	protected void checkEvents(NetworkEvent e) {
		// check notExpectedEvent List
		for (NetworkEvent notExpected : notExpectedEvents) {
			if (notExpected.equals(e)) {
				System.err.println("Not Expected Event Happened:");
				System.err.println(notExpected);
				notExpectedEvents.remove(notExpected);
				// printEvents(notExpectedEvents);
				return;
			}
		}
		// check expectedEvent List
		for (NetworkEvent expected : expectedEvents) {
			if (expected.equals(e)) {
				System.out.println("Expected Event Happened:");
				System.out.println(expected);
				expectedEvents.remove(expected);
				// printEvents(expectedEvents);
				return;
			}
		}

		System.err.println("Unknown Event:");
		System.err.println(e);
		System.out.println("*********NE***************");
		for (NetworkEvent ev : notExpectedEvents)
			System.out.println(new Gson().toJson(ev).toString());
		System.out.println("*********E***************");
		for (NetworkEvent ev : expectedEvents)
			System.out.println(new Gson().toJson(ev).toString());
		return;
	}

}

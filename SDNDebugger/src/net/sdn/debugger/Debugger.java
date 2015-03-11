package net.sdn.debugger;

/**
 * @author Da Yu, Yiming Li, Tim Nelson
 */
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import com.google.gson.Gson;

import net.sdn.event.Event;
import net.sdn.event.NetworkEvent;
import net.sdn.event.packet.OFPacket;
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

	// Separate partial lines for each connection:
	private Map<ObservableConnection<String, String>, String> partialLines =
		new HashMap<ObservableConnection<String, String>, String>();

	public Debugger() {
	}

	public static Action1<Event> func_do_nothing = new Action1<Event>() {
		@Override
		public void call(Event e) {
		}
	};

/*
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
*/

	// Extend current partial line by <msg> and extract what full lines have
	// been created.
	String[] getFullMessages(ObservableConnection<String, String> connection, String msg) {
		if(!partialLines.containsKey(connection))
			partialLines.put(connection, "");
		String partialLine = partialLines.get(connection);

		partialLine += msg;
		String temp[] = partialLine.split("\n");
		if (partialLine.endsWith("\n")) {
			// full message line
			partialLines.put(connection, "");
			return temp;
		} else {
			// part message line
			partialLines.put(connection, temp[temp.length - 1]);
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
				&& (eve.pkt.eth.ip.tcp.of_packet.of_type == OFPacket.OFPacketType.ECHO_REPLY || eve.pkt.eth.ip.tcp.of_packet.of_type == OFPacket.OFPacketType.ECHO_REQUEST);
	}

	private Observable<Event> buildNewStream(
			final ObservableConnection<String, String> connection) {
		return connection.getInput().flatMap(
		// flatMap over the stream of string chunks to create event stream
		// This func turns every string into a stream of events (possibly empty)
				new Func1<String, Observable<Event>>() {
					@Override
					public Observable<Event> call(String msg) {
						// Add the new string and see if we get any full
						// messages
						String[] fullMessages = getFullMessages(connection, msg);
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
								//timer(eve);
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

						/* Build the stream of events from this new monitor
						   Make it hot via publish/connect. Originally, this was COLD, which
						   meant that multiple subscribers to Simon.events() would cause the same
						   message to be processed multiple times.
						*/
						Observable<Event> newStream = buildNewStream(connection).publish().refCount();

						// May have multiple streams coming from multiple
						// connections, so merge them.
						events = Observable.merge(events, newStream);

						// keep going while not an error
						return connection.getInput()
								.flatMap(
										new Func1<String, Observable<Notification<Void>>>() {
											@Override
											public Observable<Notification<Void>> call(
													String str) {
												return Observable.empty();
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

}

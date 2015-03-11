import net.sdn.debugger.Debugger;

import net.sdn.event._;
import net.sdn.event.packet._;
import scala.concurrent.duration._;

// use Scala's observable here
import rx.lang.scala.Observable;
import rx.lang.scala.observables._;
import rx.lang.scala.Subscription;
import rx.lang.scala.JavaConversions;

import sys.process._
import scala.collection.mutable.SortedSet;

// Note on adding new classes that extend Event:
//  Event.toString() uses GSON to produce a JSON expression for the event.
//  Unfortunately this doesn't work well for Scala classes that inherit the method.
//  If you get a malformed class name error, make sure you've overridden the toString() method.

object Simon {
	val d: Debugger = new Debugger();
	private var running = false;
	private var lastJavaEvents: rx.Observable[Event] = rx.Observable.never();
	private var lastScalaEvents: Observable[Event] = Observable.never;

	def run() {
		if(running) return;
		println("Creating debugger object and opening monitor listener...");
		new Thread(d).start();
		running = true;
		// Dont make a fixed variable! Will never be updated when monitors join,
		// which will happen after this function terminates...
		// events = JavaConversions.toScalaObservable(d.events)
	}

	def printwarning(e: Event) {
		e match {
			case eviol: ExpectViolation => 
				println(Console.RED + "**** Violation: ****")
				println(Console.RED + eviol)
			case esucc: ExpectSuccess =>
				println(Console.GREEN + "**** Success: ****") 
				println(Console.GREEN + esucc)
			case _ =>
				println(Console.BLUE + "**** Unknown: ****")
				println(Console.BLUE + e)
		}
		// reset
		println(Console.RESET)
	}

	// WARNING: this won't auto-update existing Observables as new monitors join,
	// so wait until monitor registration is complete before calling (or be
	// aware that you may need to reconstruct streams you've already built.)
	// NOTE: this should be a *hot* Observable, i.e., it doesn't save events
	// that have happened before subscription.
	def events(): Observable[Event] = {
		// Don't create a new Observable every call.
		if(lastJavaEvents == d.events) return lastScalaEvents;
		lastJavaEvents = d.events;
		lastScalaEvents = JavaConversions.toScalaObservable(d.events);
		return lastScalaEvents;
	}
	def nwEvents(): Observable[NetworkEvent] = {
		events().flatMap(e => e match {case et: NetworkEvent => Observable.just(et) case _ => Observable.empty})
	}

	// TODO: ideally we'd have another function that created a new xterm and ran a continuous monitor until completion
	def nextEvent(o: Observable[Event]) {
		//println("Printing first event in observable (if empty, will print after something arrives):");
		//o.first.subscribe(e => println(e)); // o.first seems to become cold.

		// blocking version used here
		// don't confuse Obs.first with BlockingObs.first
		println("Printing next event to arrive on observable (will block until something arrives):");
		println(o.toBlocking.first); // BlockingObservable.first returns the event, not an Observable

		// WARNING: ^ if we do ping h1 -c 2 h2, this will only catch the first packet, and the second one will be
		// ignored even if we re-invoke. This is because events() is a hot observable.
	}

	/*
		EXPECTATIONS

		Expect to see an event matching pred within duration d.
		If this isn't seen after d, result contains an ExpectViolation.
		If this is seen before d, result contains just the event that matched.

		Note that expectation observables can be re-used. Re-subscribe to start a fresh timer and resume listening.
	*/

	def expect[EVT <: Event](src: Observable[EVT], pred: EVT=>Boolean, d: Duration): Observable[Event] = {
		return expect(src, pred, d, Observable.never)
	}
	// Cancellable expectation.
	// If the cancel observable fires an event, this expectation will never fire either way.
	def expect[EVT <: Event](src: Observable[EVT], pred: EVT=>Boolean, d: Duration, cancel: Observable[Any]): Observable[Event] = {
		// timer, filter components
		//println("Creating expectation... duration:"+d);
		val t = Observable.timer(d).map(n => new ExpectViolation());
		val f = src.filter(pred);
		//return t.merge(f).first.takeUntil(cancel);
		t.merge(f).first.takeUntil(cancel).map(e => e match {case eviol: ExpectViolation => new ExpectViolation(eviol) case e => new ExpectSuccess()})
	}
	def expectNot[EVT <: Event](src: Observable[EVT], pred: EVT=>Boolean, d: Duration): Observable[Event] = {
		return expectNot(src, pred, d, Observable.never)
	}
	def expectNot[EVT <: Event](src: Observable[EVT], pred: EVT=>Boolean, d: Duration, cancel: Observable[Any]): Observable[Event] = {
		return expect(src, pred, d, cancel).map(e => e match {case eviol: ExpectViolation => new ExpectSuccess() case e => new ExpectViolation(e)})
	}

	/*
		State management: ideal models may have internal state.
		We may also just want to store info extracted from a stream.
	*/

	def rememberInSet[EVT <: Event,T](o: Observable[EVT], s: scala.collection.mutable.SortedSet[T], f: EVT=>Option[T]): Subscription = {
		return o.subscribe({e => f(e) match { case Some(t) => s += t case None => ()}})
	}

	// packet_in, packet_out, flow_mod
	def cpRelatedTo(orig: NetworkEvent): Observable[NetworkEvent] = {
		nwEvents.filter(SimonHelper.isOFNetworkEvents).filter(e => ((e.pkt.eth.ip.tcp.of_packet.of_type == OFPacket.OFPacketType.PACKET_IN || 
																		e.pkt.eth.ip.tcp.of_packet.of_type == OFPacket.OFPacketType.PACKET_OUT) && 
																		e.pkt.eth.ip.tcp.of_packet.packet.equals(orig.pkt)) ||
																	(e.pkt.eth.ip.tcp.of_packet.of_type == OFPacket.OFPacketType.FLOW_MOD &&
																		e.pkt.eth.ip.tcp.of_packet.isMatch(orig)))
	}

/*
// prints if expectation violated
Simon.expect({e:Event => e.direction == "in"}, Duration(10, "seconds")).subscribe(e => println("result: "+e))

// to create a fresh tree set:
scala.collection.mutable.SortedSet[String]()
// to remember in that set (where it's referenced at res19):
Simon.rememberInSet[String](Simon.events(), res19, {e=>Some(e.toString())})



*/

	// note: publish turns cold into hot
}


/////////////////////////////////////////////////////
// Development notes to self
/////////////////////////////////////////////////////
/*
 sudo lets you see non-owned processes
 sudo netstat -lnptuea

./simon.sh   // loads REPL with correct classpath and auto-loads simon.scala
scala> Simon.run()

 methods return unit by default; to make them functions, add : Type = { ... }

 beware use of return inside anonymous functions; they will escape the closest *NAMED* func

 Had silent failure instantiating ExpectViolation() in expect, because new ExpectViolation()
 threw malformed class name. Not sure why.
*/

/*
How to replay? Cache can start to cache when the first subscribe happens.
> Simon.nwEvents.cache.subscribe(e => println(e))

Now, we get res11 and replay 

> res11.subscribe(e => println(e)) // print again
*/

Simon.run()
println("Simon has been loaded!")


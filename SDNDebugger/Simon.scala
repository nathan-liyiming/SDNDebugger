import net.sdn.debugger.Debugger;
import net.sdn.debugger.EmptyEvent;

// TODO: issues creating this class if defined in Scala?
import net.sdn.debugger.ExpectViolation;
import net.sdn.event.Event;

// for timers
import scala.concurrent.duration._;

// use Scala's observable here
import rx.lang.scala.Observable;
import rx.lang.scala.JavaConversions;

// Note on adding new classes that extend Event:
//  Event.toString() uses GSON to produce a JSON expression for the event. 
//  Unfortunately this doesn't work well for Scala classes that inherit the method.
//  If you get a malformed class name error, make sure you've overridden the toString() method.

object Simon {
	var d: Debugger = new Debugger();
	private var running = false;
	
	def run() {
		if(running) return;
		println("Creating debugger object and opening monitor listener...");
		new Thread(d).start();
		running = true;
		// Dont make a fixed variable! Will never be updated when monitors join,
		// which will happen after this function terminates...
		// events = JavaConversions.toScalaObservable(d.events)		
	}

	// WARNING: this won't auto-update existing Observables as new monitors join,
	// so wait until monitor registration is complete before calling (or be 
	// aware that you may need to reconstruct streams you've already built.)
	// NOTE: this should be a *hot* Observable, i.e., it doesn't save events 
	// that have happened before subscription. 	
	def events(): Observable[Event] = {		
		JavaConversions.toScalaObservable(d.events)		
	}

	// TODO: ideally we'd have another function that created a new xterm and ran a continuous monitor until completion
	def showmenext(o: Observable[Event]) {
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

	def expect(pred: Event=>Boolean, d: Duration): Observable[Event] = {
		return expect(pred, d, Observable.never)
	}
	// Cancellable expectation. 
	// If the cancel observable fires an event, this expectation will never fire either way.
	def expect(pred: Event=>Boolean, d: Duration, cancel: Observable[AnyVal]): Observable[Event] = {
		// timer, filter components 
		val t = Observable.timer(d).map(n => new ExpectViolation());
		val f = events().filter(pred); 		
		return t.merge(f).first.takeUntil(cancel); 
	}	
	


	
/*
// prints if expectation violated
Simon.expect({e:Event => e.direction == "in"}, Duration(10, "seconds")).subscribe(e => println("result: "+e))




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


		

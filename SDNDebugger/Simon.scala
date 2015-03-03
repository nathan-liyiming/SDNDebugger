import net.sdn.debugger.Debugger;
import net.sdn.debugger.EmptyEvent;
import net.sdn.event.Event;

// for timers
import scala.concurrent.duration._;

// use Scala's observable here
import rx.lang.scala.Observable;
import rx.lang.scala.JavaConversions;

// order matters on loading in REPL
class ExpectViolation() extends Event {

} 

object Simon {
	var d: Debugger = new Debugger();
	private var running = false;
	
	def main(args: Array[String]) {
		// TODO (this bit doesn't auto-run when loaded in REPL)
		// Run SIMON on module load in REPL 
		//   (don't make the user type Simon.run() every time.)
		Simon.run();
		println("SIMON loaded!");
	}

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
		// blocking version:
		// don't confuse Obs.first with BlockingObs.first
		println("Printing next event to arrive on observable (will block until something arrives):");
		println(o.toBlocking.first); // BlockingObservable.first returns the event, not an Observable

		// WARNING: ^ if we do ping h1 -c 2 h2, this will only catch the first packet, and the second one will be
		// ignored even if we re-invoke. This is because events() is a hot observable.
	}

	// Expect to see an event matching pred within duration d.
	// If this isn't seen after d, result contains an ExpectViolation. 
	// If this is seen before d, result completes.
	def expect(pred: Event=>Boolean, d: Duration): Observable[Event] = {
		// timer, filter components
		val t = Observable.timer(d);
		val f = events().filter(pred).map(e => 1); // to make types match		
		return t.merge(f).first.flatMap(
			{n => 
				if(n == 1) { println("returning empty for flatmap"+n); return Observable.empty } // saw the event, no violation
				else { println("returning violation for flatmap"+n); return Observable.just(new ExpectViolation())}}); // timer ran out
		// ^ Must be a cleaner way to write this?
	}
	
/*
// prints if expectation violated
Simon.expect({e:Event => e.direction == "in"}, Duration(10, "seconds")).subscribe({e => println("violation: "+e)})




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
*/

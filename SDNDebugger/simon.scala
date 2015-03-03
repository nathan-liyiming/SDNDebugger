import net.sdn.debugger.Debugger;
import net.sdn.debugger.EmptyEvent;
import net.sdn.event.Event;

// use scala's observable here
import rx.lang.scala.Observable;
import rx.lang.scala.JavaConversions;

object Simon {
	var d: Debugger = new Debugger();

	def run() {
		println("Creating debugger object and opening monitor listener...");
		new Thread(d).start();
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


}



// sudo lets you see non-owned processes
// sudo netstat -lnptuea

// use the classpath in runscala.sh

//scala> :load Foo.scala
// scala> Foo.main(Array())

// methods return unit by default; to make them functions, add : Type = { ... }

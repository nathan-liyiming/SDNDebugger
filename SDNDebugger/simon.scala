import net.sdn.debugger.Debugger;
import net.sdn.debugger.EmptyEvent;
import net.sdn.event.Event;

// use scala's observable here
import rx.lang.scala.Observable;
import rx.lang.scala.JavaConversions;
//import rx.Observable;


object Simon {
	var d: Debugger = new Debugger();
	var events: Observable[Event] = Observable.empty; // note no ()

	def run() {
		println("Creating debugger object and opening monitor listener...");
		new Thread(d).start();
		events = JavaConversions.toScalaObservable(d.events)
		println("Obtained observable object:"+events); // events is now a Rx Scala object 
	}

/*

	def showmefirst(o: Observable[Event]) {
		// this will give an exception if no monitors yet (i.e., d is Observable.empty())
		//o.first().subscribe(Debugger.func_printevent); // d.printevent is a RxJava Apply1 object, defined in Java		
		events.defaultIfEmpty(new EmptyEvent()).first().subscribe(Debugger.func_printevent);
	}
*/

}



// sudo lets you see non-owned processes
// sudo netstat -lnptuea

// use the classpath in runscala.sh

//scala> :load Foo.scala
// scala> Foo.main(Array())

// methods return unit by default; to make them functions, add : Type = { ... }

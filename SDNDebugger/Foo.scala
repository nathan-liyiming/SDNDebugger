import net.sdn.debugger.Debugger;

object Foo {
	def main(args: Array[String]) {
		println("Loading debugger...");

		//val po = new PhyTopo(args(0));
		//val d = new Debugger(po);
		val d = new Debugger();
		//v.addInterestedEvents(PacketType.TCP);
		//v.addInterestedEvents(PacketType.ICMP);
		//v.addInterestedEvents(PacketType.OF);
		// v.addInterestedEvents(PacketType.UDP);
		new Thread(d).start();

		println(d.events.toString());
	}

}



// sudo lets you see non-owned processes
// sudo netstat -lnptuea

// use the classpath in runscala.sh

//scala> :load Foo.scala
// scala> Foo.main(Array())



/*
	Ideal model + reactor for Stateful Firewall example


	Could be enhanced to use cancellable expectations; right now this model
	will be confused by an over-eager external host.  Consider
		(1) ext -> [expect not to see corresponding output]
		(2) int -> [expect to pass, modify state;cancel original expectation (assuming no way to distinguish (1) and (3))]
		(3) ext -> [expect to pass]

*/

/*
	With respect to a topology that defines a set of SFW switches and, for each switch, a set of internal and external ports...
		ext -> int [not in state] => expect dropped
		int -> ext => expect pass, add to state
		ext -> int [in state] => expect pass
*/

import net.sdn.phytopo.Host;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;
import net.sdn.event._;
import net.sdn.event.packet._;

class SFIdeal(topofn: String) {
	val topo = new PhyTopo(topofn);
	val allowed = new scala.collection.mutable.TreeSet[Tuple2[String, String]]();

	// TODO: this is way too verbose
	def stateHelper(e: Event): Option[Tuple2[String, String]] = {
		e match {
			case et: NetworkEvent => {
					if(et.pkt.eth == null) return None
					else return Some(new Tuple2[String,String](et.pkt.eth.dl_src, et.pkt.eth.dl_dst))
			}
			case _ => return None
		}
	}

	def printwarning(e: Event) {
		println("**** Expectation violated!");
		println(e); // TODO more detail
	}

	// Listen in for necessary state changes
	Simon.rememberInSet[Tuple2[String,String]](Simon.events(), allowed, stateHelper);

	// ext -> int [not in state] => expect dropped

	// int -> ext => expect pass, add to state
	// TODO: is flatMap safe here?
	Simon.events().filter(is_incoming_int).flatMap(e =>
		e match
			case et: NetworkEvent =>
				Simon.expect({e2 => /* outgoing, same sw, same addrs... et.pkt.eth.dl_src*/}, Duration(10, "milliseconds"))).subscribe(printwarning);



	// ext -> int [in state] => expect pass
}
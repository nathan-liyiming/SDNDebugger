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
import scala.concurrent.duration._;

class SFIdeal(topofn: String) {
	val topo = new PhyTopo(topofn);
	val allowed = new scala.collection.mutable.TreeSet[Tuple2[String, String]]();

	def printwarning(e: Event) {
		println("**** Expectation violated!");
		println(e); // TODO more detail
	}

	//
	def is_outgoing_same(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e =>
			e.pkt.eth.dl_src == orig.pkt.eth.dl_src &&
			e.pkt.eth.dl_dst == orig.pkt.eth.dl_dst &&
			e.sw == orig.sw && e.direction == NetworkEventDirection.OUT
		}
	}
	def is_incoming_int(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN && true // todo
	}
	def is_incoming_ext_allowed(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN && true // todo
	}
	def is_incoming_ext_not_allowed(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN && true // todo
	}


	// Listen in for necessary state changes
	Simon.rememberInSet(Simon.nwEvents(), allowed,
		{e: NetworkEvent =>Some(new Tuple2[String,String](e.pkt.eth.dl_src, e.pkt.eth.dl_dst))});

	// Note: flatMap will _merge_ the streams that come out of each map. So we want flatMap after all.
	//       concatMap would _concat_ them, which we don't want.
	//        [apparently, with concatMap, subscriptions may only happen after the previous stream terminates,
	//          which can cause problems for concatMapping hot observables. (Source: stackoverflow.)]

	// e1: ext -> int [not in state] => expect dropped
	val e1 = Simon.nwEvents().filter(is_incoming_ext_not_allowed).flatMap(e =>
				Simon.expectNot(Simon.nwEvents(), is_outgoing_same(e), Duration(10, "milliseconds")));

	// e2: int -> ext => expect pass, add to state
	val e2 = Simon.nwEvents().filter(is_incoming_int).flatMap(e =>
				Simon.expect(Simon.nwEvents(), is_outgoing_same(e), Duration(10, "milliseconds")));

	// e3: ext -> int [in state] => expect pass
	val e3 = Simon.nwEvents().filter(is_incoming_ext_allowed).flatMap(e =>
				Simon.expect(Simon.nwEvents(), is_outgoing_same(e), Duration(10, "milliseconds")));

	val violations = e1.merge(e2).merge(e3);
	val autosubscribe = violations.subscribe({e => printwarning(e)});
}
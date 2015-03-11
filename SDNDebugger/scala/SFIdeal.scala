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
import scala.collection.immutable.Set;

/*
	Pass in a switch ID that is the firewall and a set of internal/external ports.
	TODO: should take set of switches and mappings for internal/external.
*/
class SFIdeal(fwswitchid: String, fwinternals: Set[String], fwexternals: Set[String]) {
	//val topo = new PhyTopo(topofn);
	
	// use (dst,src) for incoming from external; this records outgoing pairs.
	val allowed = new scala.collection.mutable.TreeSet[Tuple2[String, String]]();

	//
	def isOutSame(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e =>
			e.pkt.eth.dl_src == orig.pkt.eth.dl_src &&
			e.pkt.eth.dl_dst == orig.pkt.eth.dl_dst &&
			e.sw == orig.sw && e.pkt.eth.ip.icmp.sequence == orig.pkt.eth.ip.icmp.sequence &&
			e.direction == NetworkEventDirection.OUT
		}
	}

	def inOppositeSrcdst(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e =>
			e.pkt.eth.dl_src == orig.pkt.eth.dl_dst &&
			e.pkt.eth.dl_dst == orig.pkt.eth.dl_src &&
			e.sw == orig.sw && e.direction == NetworkEventDirection.IN
		}
	}

	// TODO: OMGZ! Make interf field hold single interface!
	//   (if this event ever arrives with >1 interface in the list, this check will *fail*)
	// Also: empty list (for ctrler msg) ---> saved by short circuit evaluation below
	def isInInt(e: NetworkEvent): Boolean = {		
		/*println( (e.direction == NetworkEventDirection.IN));
		println(e.sw == fwswitchid); 
		println(if(e.interf.size() >= 1) (fwinternals(e.interf.get(0))) else "NONE");*/
			
		e.direction == NetworkEventDirection.IN && // short circuit
		e.sw == fwswitchid && fwinternals(e.interf)
	}
	def isInExtAllow(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN && 
		e.sw == fwswitchid && fwexternals(e.interf) && allowed((e.pkt.eth.dl_dst, e.pkt.eth.dl_src))
	}
	def isInExtNAllow(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN && 
		e.sw == fwswitchid && fwexternals(e.interf) && !allowed((e.pkt.eth.dl_dst, e.pkt.eth.dl_src))
	}

	// we only handle icmp flow
	val ICMPStream = Simon.nwEvents().filter(SimonHelper.isICMPNetworkEvents)

	// Listen in for necessary state changes
	Simon.rememberInSet(ICMPStream, allowed,
		{e: NetworkEvent => if(isInInt(e)) 
								Some(new Tuple2[String,String](e.pkt.eth.dl_src, e.pkt.eth.dl_dst))
							else None});

	// Note: flatMap will _merge_ the streams that come out of each map. So we want flatMap after all.
	//       concatMap would _concat_ them, which we don't want.
	//        [apparently, with concatMap, subscriptions may only happen after the previous stream terminates,
	//          which can cause problems for concatMapping hot observables. (Source: stackoverflow.)]

	// e1: ext -> int [not in state] => expect dropped

	val e1 = ICMPStream.filter(isInExtNAllow).flatMap(e =>
				Simon.expectNot(ICMPStream, isOutSame(e), Duration(100, "milliseconds"), ICMPStream.filter(inOppositeSrcdst(e))));

	// e2: int -> ext => expect pass, add to state
	val e2 = ICMPStream.filter(isInInt).flatMap(e =>
				Simon.expect(ICMPStream, isOutSame(e), Duration(100, "milliseconds")));

	// e3: ext -> int [in state] => expect pass
	val e3 = ICMPStream.filter(isInExtAllow).flatMap(e =>
				Simon.expect(ICMPStream, isOutSame(e), Duration(100, "milliseconds")));

	val violations = e1.merge(e2).merge(e3);
	val autosubscribe = violations.subscribe({e => Simon.printwarning(e)});
}

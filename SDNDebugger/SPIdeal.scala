import net.sdn.phytopo.Host
import net.sdn.phytopo.PhyTopo
import net.sdn.phytopo.Switch
import net.sdn.event._
import net.sdn.event.packet._
import net.sdn.event.util.BellmanFord
import net.sdn.event.util.BellmanFord.Pair
import scala.collection.mutable.Map
import scala.collection.JavaConversions._

import rx.lang.scala.Observable

class SPIdeal(topofn: String) {
	val topo = new PhyTopo(topofn)
	val routes = BellmanFord.bellmanFordCompute(topo)
	val count = Map[BellmanFord.Pair, Int]()

	def printwarning(e: Event) {
		e match {
			case eviol: ExpectViolation => 
				println("**** Violation: ****")
				println(eviol)
			case esucc: ExpectSuccess =>
				println("**** Success: ****") 
				println(esucc)
			case _ =>
				println("**** Unknown: ****")
				println(e)
		}
	}

	def isComingIn(e: NetworkEvent): Boolean = {
		e.direction == NetworkEventDirection.IN
	}

	def isPassThrough(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e => {
			e.pkt.eth.ip.nw_src == orig.pkt.eth.ip.nw_src &&
			e.pkt.eth.ip.nw_dst == orig.pkt.eth.ip.nw_dst &&
			e.sw == orig.sw && e.direction == NetworkEventDirection.OUT
			}
		}
	}

	def isFirstHop(e: NetworkEvent): Boolean = {
		if (e.direction == NetworkEventDirection.IN) {
			for (host <- topo.getSwitch(e.sw).getAttachedHosts()) {
				if (topo.getHost(host).getNwAddr() == e.pkt.eth.ip.nw_src) {
					return true
				}
			}
		}
		return false
	}

	def isLastHop(e: NetworkEvent): Boolean = {
		if (e.direction == NetworkEventDirection.OUT) {
			for (host <- topo.getSwitch(e.sw).getAttachedHosts()) {
				if (topo.getHost(host).getNwAddr() == e.pkt.eth.ip.nw_dst) {
					val b = new BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
					return true
				}
			}
		}
		return false
	}

	def setCounter(e: NetworkEvent) {
		val b = new BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
		if (isFirstHop(e)) {
			count(b) = routes.get(b).size() / 2
		}
		
		count(b) = count(b) - 1
	}

	def expectCounterZero(e: NetworkEvent): Event = {
		val b = new BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
		count(b) == 0 match {case true => new ExpectSuccess() case false => new ExpectViolation(e)}
	}

	val ICMPStream = Simon.nwEvents().filter(SimonHelper.isICMPNetworkEvents)

	ICMPStream.filter(isComingIn).subscribe(setCounter(_))

	val e1 = ICMPStream.filter(isComingIn).flatMap(e =>
				Simon.expect(ICMPStream, isPassThrough(e), Duration(100, "milliseconds")));

	val e2 = ICMPStream.filter(isLastHop).map(expectCounterZero(_))

	val violations = e1.merge(e2)
	
	val autosubscribe = violations.subscribe(printwarning(_))
}

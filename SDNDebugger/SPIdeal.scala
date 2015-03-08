import net.sdn.phytopo.Host
import net.sdn.phytopo.PhyTopo
import net.sdn.phytopo.Switch
import net.sdn.event._
import net.sdn.event.packet._
import net.sdn.event.util.BellmanFord
import net.sdn.event.util.BellmanFord.Triple
import scala.collection.mutable.Map

class SPIdeal(topofn: String) {
	val topo = new PhyTopo(topofn)
	val routes = BellmanFord.bellmanFordCompute(topo)
	val count = Map[BellmanFord.Pair, Int]()

	def isPassThrough(orig: NetworkEvent): NetworkEvent => Boolean = {
		{e => {
			val b = BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
			count(b) = count(b) - 1
			e.pkt.eth.ip.nw_src == orig.pkt.eth.ip.nw_src &&
			e.pkt.eth.ip.nw_dst == orig.pkt.eth.ip.nw_dst &&
			e.sw == orig.sw && e.direction == NetworkEventDirection.OUT
			}
		}
	}

	def isFirstHop(e: NetworkEvent): Boolean {
		if (e.direction == NetworkEventDirection.IN) {
			for (host <- topo.getSwitch(e.sw).getAttachedHosts()) {
				if (topo.getHost(host).getNwAddr() == e.nw_src) {
					return true
				}
			}
		}
		return false
	}

	def isLastHop(e: NetworkEvent): Boolean {
		if (e.direction == NetworkEventDirection.OUT) {
			for (host <- topo.getSwitch(e.sw).getAttachedHosts()) {
				if (topo.getHost(host).getNwAddr() == e.nw_dst) {
					return true
				}
			}
		}
		return false
	}

	def setCounter(e: NetworkEvent) {
		val b = BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
		count(b) = routes.get(b).size() / 2
	}

	def isCounterZero(e: NetworkEvent): Boolean = {
		val b = BellmanFord.Pair(e.pkt.eth.ip.nw_src, e.pkt.eth.ip.nw_dst)
		count(b) == 0
	}

	val ICMPStream = Simon.nwEvents().filter(SimonHelper.isICMPNetworkEvents)

	ICMPStream.filter(isFirstHop).subscribe(setCounter)

	ICMPStream.filter(e => e.direction == NetworkEventDirection.IN).flatMap(e =>
				Simon.expect(ICMPStream, isPassThrough(e), Duration(100, "milliseconds")));

	ICMPStream.filter(isLastHop).flatMap(e =>
				Simon.expect(ICMPStream, isCounterZero, Duration(100, "milliseconds")))
}

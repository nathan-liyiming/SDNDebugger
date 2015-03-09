package net.sdn.debugger.oldversion;

import java.util.HashMap;
import java.util.List;

import net.sdn.event.NetworkEvent;
import net.sdn.event.NetworkEventDirection;
import net.sdn.event.packet.PacketType;
import net.sdn.event.util.BellmanFord;
import net.sdn.event.util.BellmanFord.Triple;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class SPVerifier extends Verifier {
	private PhyTopo phyTopo = null;
	private HashMap<BellmanFord.Pair, List<Triple>> routes = null;
	private HashMap<BellmanFord.Pair, Integer> count = new HashMap<BellmanFord.Pair, Integer>();

	public SPVerifier(PhyTopo phytopo) {
		phyTopo = phytopo;
		routes = calcSP();
	}

	@Override
	public void verify(NetworkEvent event) {
		// ideal model
		String nw_src = event.pkt.eth.ip.nw_src;
		String nw_dst = event.pkt.eth.ip.nw_dst;
		Switch s = phyTopo.getSwitch(event.sw);

		BellmanFord.Pair b = new BellmanFord.Pair(nw_src, nw_dst);
		// check whether the event is in
		if (event.direction == NetworkEventDirection.IN) {
			// get all attached hosts
			for (String host : s.getAttachedHosts()) {
				// first hop, generate events
				if (phyTopo.getHost(host).getNwAddr().equalsIgnoreCase(nw_src)) {
					List<Triple> route = routes.get(b);
					count.put(b, route.size() - 1);
					return;
				}
			}

			if (count.containsKey(b)) {
				count.put(b, count.get(b) - 1);
			} else {
				System.out.println("Unknown event: " + event);
			}
		}

		// check whether the event is out
		if (event.direction == NetworkEventDirection.OUT) {
			// get all attached hosts
			for (String host : s.getAttachedHosts()) {
				// final hop, generate events
				if (phyTopo.getHost(host).getNwAddr().equalsIgnoreCase(nw_dst)) {
					if (count.containsKey(b) && count.get(b) == 1) {
						System.out
								.println("Correct, this is one shortest path from "
										+ nw_src
										+ " to "
										+ nw_dst
										+ " for "
										+ event.pkt.eth.ip.icmp.op + "!");
					} else {
						System.out.println("This is not shortest path!");
					}
					return;
				}
			}

			if (count.containsKey(b)) {
				count.put(b, count.get(b) - 1);
			} else {
				System.out.println("Unknown event: " + event);
			}
		}
	}

	public HashMap<BellmanFord.Pair, List<Triple>> calcSP() {
		return BellmanFord.bellmanFordCompute(phyTopo);
	}

	public static void main(String[] args) {
		Verifier v = new SPVerifier(new PhyTopo(args[0]));
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.ICMP);
		// v.addInterestedEvents(PacketType.UDP);
		new Thread(v).start();
	}

}

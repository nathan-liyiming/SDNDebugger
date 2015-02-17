package net.sdn.debugger;

import java.util.HashMap;
import java.util.List;

import net.sdn.event.Event;
import net.sdn.event.EventGenerator;
import net.sdn.event.packet.PacketType;
import net.sdn.event.util.BellmanFord;
import net.sdn.event.util.BellmanFord.Triple;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class SPVerifier extends Verifier {
	private PhyTopo phyTopo = null;
	private HashMap<BellmanFord.Pair, List<Triple>> routes = null;

	public SPVerifier(PhyTopo phytopo) {
		phyTopo = phytopo;
		routes = calcSP();
	}

	@Override
	public void verify(Event event) {
		// ideal model
		String nw_src = event.pkt.eth.ip.nw_src;
		String nw_dst = event.pkt.eth.ip.nw_dst;
		Switch s = phyTopo.getSwitch(event.sw);
		
		// check whether the event is in
		if (event.direction.equalsIgnoreCase("in")){
			// get all attached hosts
			for (String host : s.getAttachedHosts()) {
				// first hop, generate events
				if (phyTopo.getHost(host).getNwAddr().equalsIgnoreCase(nw_src)) {
					List<Triple> route = routes.get(new BellmanFord.Pair(nw_src, nw_dst));
					for (int i = 1; i < route.size(); i++){
						Triple hop = route.get(i);
						addExpectedEvents(EventGenerator.generateEvent(
								event.priority, event.pkt, hop.sw.getId(), hop.interf, hop.direction, event.timeStamp));
					}
					return;
				}
			}
		}
		
		checkEvents(event);
	}
	
	public HashMap<BellmanFord.Pair, List<Triple>> calcSP() {
		return BellmanFord.bellmanFordCompute(phyTopo);
	}

	public static void main(String[] args) {
		Verifier v = new SPVerifier(new PhyTopo(args[0]));
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.ICMP);
		//v.addInterestedEvents(PacketType.UDP);
		new Thread(v).start();
	}

}

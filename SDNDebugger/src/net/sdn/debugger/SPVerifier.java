package net.sdn.debugger;

import java.util.HashMap;
import java.util.List;

import net.sdn.event.Event;
import net.sdn.event.packet.PacketType;
import net.sdn.event.util.BellmanFord;
import net.sdn.event.util.BellmanFord.Triple;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class SPVerifier extends Verifier {
	private PhyTopo phyTopo = null;

	public SPVerifier(PhyTopo phytopo) {
		phyTopo = phytopo;
		calcSP();
	}

	@Override
	public void verify(Event event) {
		// ideal model

		// edge switch generate expected events
		if (true) {

		} else {
			checkEvents(event);
		}
	}

	public HashMap<BellmanFord.Pair, List<Triple>> calcSP() {
		return BellmanFord.bellmanFordCompute(phyTopo);
	}

	public static void main(String[] args) {
		Verifier v = new SPVerifier(new PhyTopo(args[0]));
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.UDP);
	}

}

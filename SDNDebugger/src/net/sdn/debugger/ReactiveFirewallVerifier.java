package net.sdn.debugger;

import net.sdn.event.Event;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.PhyTopo;

public class ReactiveFirewallVerifier extends Verifier {

	@Override
	public void verifier(Event event) {
		// TODO Auto-generated method stub
		System.out.println(event.toString());
	}

	public static void main(String[] args) {
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new ReactiveFirewallVerifier();
		v.addPhyTopo(po);
		v.addInterestedEvents(PacketType.OF);
		new Thread(v).start();
	}
}
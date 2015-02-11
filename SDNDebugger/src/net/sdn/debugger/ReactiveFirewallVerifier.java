package net.sdn.debugger;

import net.sdn.event.Event;
import net.sdn.event.EventGenerator;
import net.sdn.event.packet.Packet;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;
import net.sdn.policy.Policy;

public class ReactiveFirewallVerifier extends Verifier {

	@Override
	public void verifier(Event event) {
		// ideal model
		PhyTopo phyTopo = getPhyTopo();
		Packet pkt = event.pkt;

		Switch s1 = phyTopo.getSwitch("s1");
		if (event.direction.equals("in")) {
			for (Policy p : s1.getPolicies()) {
				// ALLOW
				if (p.isMatched(pkt)) {
					if (p.action.equals("ALLOW")) {
						addExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, "s1", s1.getAllPorts()));
					} else {
						addNotExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, "s1", s1.getAllPorts()));
					}
					return;
				}
			}

			// default drop
			addNotExpectedEvents(EventGenerator.generateEvent(
					Event.DEFAULT_PRIORITY, pkt, "s1", s1.getAllPorts()));
		} else {
			verify(event);
		}
	}

	public void changeInternalState(Event e) { // TODO: captured REST API from 8080

	}

	public static void main(String[] args) {
		// second argument can be used to define granularity
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new ReactiveFirewallVerifier();
		v.addPhyTopo(po);
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.UDP);
		v.addInterestedEvents(PacketType.ICMP);
		new Thread(v).start();
	}
}
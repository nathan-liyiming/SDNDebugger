package net.sdn.debugger;

import java.util.HashSet;
import java.util.Set;

import net.sdn.event.Event;
import net.sdn.event.EventGenerator;
import net.sdn.event.packet.Packet;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class SFVerifier extends Verifier {
	private PhyTopo phyTopo;
	private Set<String> allowInterfs = new HashSet<String>();
	private Switch firewallSwitch;

	public SFVerifier(PhyTopo phytopo, String allowInterf, Switch s) {
		phyTopo = phytopo;
		allowInterfs.add(allowInterf);
		firewallSwitch = s;
	}

	@Override
	public void verify(Event event) {
		// ideal model
		// check whether the event is in
		if (event.direction.equalsIgnoreCase("in")) {
			String interf = event.interf.get(0);
			Packet pkt = event.pkt;
			if (allowInterfs.contains(interf)) {
				// ALLOW
				addExpectedEvents(EventGenerator.generateEvent(0, pkt,
						firewallSwitch.getId(),
						firewallSwitch.getAllPortsExcept(interf), "out",
						event.timeStamp));
				// Add the allowing interface
			} else {
				// DROP
				addNotExpectedEvents(EventGenerator.generateEvent(0, pkt,
						firewallSwitch.getId(),
						firewallSwitch.getAllPortsExcept(interf), "out",
						event.timeStamp));
			}
		} else {
			if (checkEvents(event)) {
				allowInterfs.add(event.interf.get(0));
			}
		}
	}

	public static void main(String[] args) {
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new SFVerifier(po, "eth1", po.getSwitch("s1"));
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.ICMP);
		// v.addInterestedEvents(PacketType.UDP);
		new Thread(v).start();
	}
}

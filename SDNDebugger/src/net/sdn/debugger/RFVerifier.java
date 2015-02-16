package net.sdn.debugger;

import java.util.Iterator;

import com.google.gson.Gson;

import net.sdn.event.Event;
import net.sdn.event.EventGenerator;
import net.sdn.event.packet.Packet;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;
import net.sdn.policy.Policy;

public class RFVerifier extends Verifier {
	
	private Switch firewallSwitch = null;
	private PhyTopo phyTopo = null;
	
	public RFVerifier(PhyTopo phytopo, Switch s){
		phyTopo = phytopo;
		firewallSwitch = s;
	}

	@Override
	public void verify(Event event) {
		// ideal model
		Packet pkt = event.pkt;

		if (pkt.eth != null && pkt.eth.ip != null && pkt.eth.ip.tcp != null
				&& pkt.eth.ip.tcp.tcp_dst.equals("8080")
				&& pkt.eth.ip.tcp.payload != null) {
			changeInternalState(event);
			return;
		}

//		Switch s1 = phyTopo.getSwitch("s1");
		if (event.direction.equals("in")) {
			for (Policy p : firewallSwitch.getPolicies()) {
				if (p.isMatched(pkt)) {
					// DROP
					if (p.actions.equalsIgnoreCase("DENY")) {
						addNotExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, firewallSwitch.getId(),
								firewallSwitch.getAllPortsExcept(event.interf.get(0)),
								"out", event.timeStamp));
					} else {
						// ALLOW
						addExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, firewallSwitch.getId(),
								firewallSwitch.getAllPortsExcept(event.interf.get(0)),
								"out", event.timeStamp));
					}
					return;
				}
			}

			// default drop
			addNotExpectedEvents(EventGenerator.generateEvent(
					Event.DEFAULT_PRIORITY, pkt, firewallSwitch.getId(),
					firewallSwitch.getAllPortsExcept(event.interf.get(0)), "out",
					event.timeStamp));
		} else {
			checkEvents(event);
		}
	}

	public void changeInternalState(Event e) { 
		// System.out.println(new String(e.pkt.eth.ip.tcp.payload));
		Policy p = new Gson().fromJson(new String(e.pkt.eth.ip.tcp.payload),
				Policy.class);
		phyTopo.addPolicyToSwitch(firewallSwitch.getId(), p);

		// solve conflicts
		if (p.actions != null && p.actions.equalsIgnoreCase("deny")) {
			// remove expectedEvents
			Iterator<Event> eIt = expectedEvents.iterator();
			while (eIt.hasNext()) {
				Event temp = eIt.next();
				if (!temp.sw.equalsIgnoreCase(firewallSwitch.getId())
						|| temp.priority > p.priority) {
					continue;
				} else {
					if (p.isMatched(temp.pkt))
						eIt.remove();
				}
			}
		} else {
			// remove notexpectedevents
			Iterator<Event> eIt = notExpectedEvents.iterator();
			while (eIt.hasNext()) {
				Event temp = eIt.next();
				if (!temp.sw.equalsIgnoreCase(firewallSwitch.getId())
						|| temp.priority > p.priority) {
					continue;
				} else {
					if (p.isMatched(temp.pkt))
						eIt.remove();
				}
			}
		}
	}

	public static void main(String[] args) {
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new RFVerifier(po, po.getSwitch("s1"));
		v.addInterestedEvents(PacketType.TCP);
		// v.addInterestedEvents(PacketType.UDP);
		v.addInterestedEvents(PacketType.ICMP);
		new Thread(v).start();
	}
}
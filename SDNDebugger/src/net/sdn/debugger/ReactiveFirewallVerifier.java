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

public class ReactiveFirewallVerifier extends Verifier {

	@Override
	public void verify(Event event) {
		// System.out.println(event);
		// ideal model
		PhyTopo phyTopo = getPhyTopo();
		Packet pkt = event.pkt;

		if (pkt.eth != null && pkt.eth.ip != null && pkt.eth.ip.tcp != null
				&& pkt.eth.ip.tcp.tcp_dst.equals("8080")
				&& pkt.eth.ip.tcp.payload != null) {
			changeInternalState(event);
			return;
		}

		Switch s1 = phyTopo.getSwitch("s1");
		if (event.direction.equals("in")) {
			for (Policy p : s1.getPolicies()) {
				if (p.isMatched(pkt)) {
					// DROP
					if (p.actions.equalsIgnoreCase("DENY")) {
						addNotExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, "s1",
								s1.getAllPortsExcept(event.interf.get(0)),
								"out", event.timeStamp));
					} else {
						// ALLOW
						addExpectedEvents(EventGenerator.generateEvent(
								p.priority, pkt, "s1",
								s1.getAllPortsExcept(event.interf.get(0)),
								"out", event.timeStamp));
					}
					return;
				}
			}

			// default drop
			addNotExpectedEvents(EventGenerator.generateEvent(
					Event.DEFAULT_PRIORITY, pkt, "s1",
					s1.getAllPortsExcept(event.interf.get(0)), "out",
					event.timeStamp));
		} else {
			checkEvents(event);
		}
	}

	public void changeInternalState(Event e) { // TODO: captured REST API from
												// 8080
		// System.out.println(new String(e.pkt.eth.ip.tcp.payload));
		Policy p = new Gson().fromJson(new String(e.pkt.eth.ip.tcp.payload),
				Policy.class);
		PhyTopo phyTopo = getPhyTopo();
		phyTopo.addPolicyToSwitch("s1", p);

		// solve conflicts
		if (p.actions != null && p.actions.equalsIgnoreCase("deny")) {
			// remove expectedEvents
			Iterator<Event> eIt = expectedEvents.iterator();
			while (eIt.hasNext()) {
				Event temp = eIt.next();
				if (!temp.sw.equalsIgnoreCase("s1")
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
				if (!temp.sw.equalsIgnoreCase("s1")
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
		// second argument can be used to define granularity
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new ReactiveFirewallVerifier();
		v.addPhyTopo(po);
		v.addInterestedEvents(PacketType.TCP);
		// v.addInterestedEvents(PacketType.UDP);
		v.addInterestedEvents(PacketType.ICMP);
		new Thread(v).start();
	}
}
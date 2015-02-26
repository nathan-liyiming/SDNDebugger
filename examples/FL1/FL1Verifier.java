/*
	Test using debugger with Flowlog (i.e., less setup time to build test program)
	Should not have inconsistent packet-outs due to tierlessness, but other interesting properties exist.

	TODO: OF 1.0 OK?
	TODO: multiple verifiers at once?
	TODO: filter using proper reactive functions? processing functions?
	TODO: see if we can add some helpers/etc. to clean up syntax even more
	    (this will improve examples in paper)

	TODO: why such a limited notion of "interesting" event?

	TODO: why is the verifier a void function?

*/

package net.sdn.debugger;

import java.util.HashSet;
import java.util.Set;

import net.sdn.event.Event;
import net.sdn.event.EventGenerator;
import net.sdn.event.packet.Packet;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.Host;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class FL1Verifier extends Verifier {
	private PhyTopo phyTopo;
	private Set<String> sourcesSeen = new HashSet<String>();

	public FL1Verifier(PhyTopo phytopo) {
		this.phyTopo = phytopo;
	}

	@Override
	public void verify(Event event) {

		System.out.println("verifier received: "+event.toString());

		/*
			Verify: after <src> appears on the network, packets from <src> never go to the controller
				once packetOut for that <src> seen. [Tests FL properly sending packetOut before FlowMod, etc.]

			That means a two-stage management of expectations:
			  if this <src> hasn't been seen before, expect a packetout.
			  seeing that packetout triggers expecting never to see an out to controller.

			FW: Seems this would be suited to LTL statements. "until bleh, expect foo..."
		*/
		// TODO: why is the direction field still a string?

		if(event.direction.equalsIgnoreCase("in")) {
			if(sourcesSeen.contains(event.pkt.eth.dl_src)) return;

			// Remember that the network has now seen this source address
			sourcesSeen.add(event.pkt.eth.dl_src);


		}

		//

/*		if (event.direction.equalsIgnoreCase("in") && event.sw.equalsIgnoreCase(firewallSwitch.getId())) {
			String interf = event.interf.get(0);
			Packet pkt = event.pkt;
			if (allowInterfs.contains(interf)) {
				// ALLOW
				addExpectedEvents(EventGenerator.generateEvent(0, pkt,
						firewallSwitch.getId(),
						firewallSwitch.getAllPortsExcept(interf), "out",
						event.timeStamp));
				// Add the allowing interface
				Host h = phyTopo.getHostByMAC(event.pkt.eth.dl_dst);
				allowInterfs.add(h.getAttachedSwitchInterf());

			} else {
				// DROP
				addNotExpectedEvents(EventGenerator.generateEvent(0, pkt,
						firewallSwitch.getId(),
						firewallSwitch.getAllPortsExcept(interf), "out",
						event.timeStamp));
			}
		} else {
				checkEvents(event);
		}*/
	}

	public static void main(String[] args) {
		PhyTopo po = new PhyTopo(args[0]);
		Verifier v = new FL1Verifier(po);
		v.addInterestedEvents(PacketType.TCP);
		v.addInterestedEvents(PacketType.ICMP);
		v.addInterestedEvents(PacketType.OF);
		// v.addInterestedEvents(PacketType.UDP);
		new Thread(v).start();
	}
}

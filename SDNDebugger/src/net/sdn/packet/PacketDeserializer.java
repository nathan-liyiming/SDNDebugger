package net.sdn.packet;

public class PacketDeserializer {

	public static Packet deserialize(String record) {
		Packet pkt = new Packet();
		String[] array = record.split("\t");

		pkt.dl_src = array[0];
		pkt.dl_dst = array[1];
		pkt.nw_src = array[2];
		pkt.nw_dst = array[3];
		
		if (!array[4].isEmpty()) {
			String ports[] = array[4].split(",");
			pkt.tp_src_port = ports[0];
			pkt.tp_dst_port = ports[1];
		}
		
		if (array[5].contains("arp")) {
			pkt.dl_proto = "arp";
		} else if (array[5].contains("icmp")) {
			pkt.dl_proto = "icmp";			
		} else if (array[5].contains("tcp")) {
			// payload maybe openflow
			pkt.dl_proto = "ip";
			pkt.nw_proto = "tcp";			
		} else if (array[5].contains("udp")) {
			// payload maybe bootp
			pkt.dl_proto = "ip";
			pkt.nw_proto = "udp";				
		}
		
		if (!array[6].isEmpty()) {
			pkt.of_type = Integer.parseInt(array[6]);
		} else {
			pkt.of_type = -1;
		}
		
		// if it is of message, interface will be one switch
		pkt.interf_switch = array[7];
		return pkt;
	}

}
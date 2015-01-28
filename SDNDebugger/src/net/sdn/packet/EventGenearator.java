package net.sdn.packet;

public class EventGenearator {
	
	public static double ass = -1;

	public static Event deserialize(String record) {
		Event eve = new Event();
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
			pkt.nw_proto = "icmp";			
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
		if (array[7].contains("-"))
			eve.interf = array[7].split("-")[1];
			
		eve.sw = array[7].split("-")[0];
		
		eve.timeStamp = array[8];
		
		// for test gurantee no out of order event
		if (Double.parseDouble(array[8]) <= ass)
			System.err.println("Error: OUT OF ORDER EVENT: " + array[8]);
		
		eve.pkt = pkt;
		return eve;
	}

}

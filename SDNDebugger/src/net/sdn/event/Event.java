package net.sdn.event;

public class Event {
	public Packet pkt;
	public String sw = "";
	public String interf = "";
	public String timeStamp = "";
	
	public String toString(){
		if (pkt.of_type < 0)
			return timeStamp + "{" + pkt.dl_proto + ":" + pkt.dl_src + "->" + pkt.dl_dst + ","
					+ pkt.dl_proto + ":" + pkt.nw_src + "->" + pkt.nw_dst + "}" 
					+ " -> " + sw + " : " + interf;
		else{
			if (pkt.tp_dst_port.equals("6633")){
				return timeStamp + "{" + pkt.of_type + ":" + pkt.dl_proto + "," + pkt.nw_proto + "}:" + sw + "->" + "controller";
			} else {
				return timeStamp + "{" + pkt.of_type + ":" + pkt.dl_proto + "," + pkt.nw_proto + "}:" + "controller" + "->" + sw;
			}
		}
	}
	
	
	public boolean equals(Event e){
		if (pkt.equals(e.pkt) && sw.equals(e.sw) && interf.equals(e.interf))
			return true;
		else
			return false;
	}
}

package net.sdn.event;

public class Packet {
	/*
		public Ethernet eth;
		public ARP arp;
		public IP ip;
		public ICMP icmp;
		public TCP tcp;
		public UDP udp;
	*/
	// default is null
	public String dl_src = "";
	public String dl_dst = "";
	public String dl_proto = "";
	public String nw_src = "";
	public String nw_dst = "";
	public String nw_proto = "";
	public String tp_src_port = "";
	public String tp_dst_port = "";
	public int of_type; //  -e mean 
	public Packet inner_packet;
	
	public String toString() {
		return  dl_src + "\t" +
				dl_dst + "\t" +
				dl_proto + "\t" +
				nw_src + "\t" +
				nw_dst + "\t" +
				nw_proto + "\t" +
				tp_src_port + "\t" +
				tp_dst_port + "\t" +
				of_type;
	}
	
	public boolean equals(Packet p) {
		if (((dl_src.equals("") || p.dl_src.equals("")) || (dl_src.equals(p.dl_src))) &&
			((dl_dst.equals("") || p.dl_dst.equals("")) || (dl_dst.equals(p.dl_dst))) &&
			((dl_proto.equals("") || p.dl_proto.equals("")) || (dl_proto.equals(p.dl_proto))) &&
			((nw_src.equals("") || p.nw_src.equals("")) || (nw_src.equals(p.nw_src))) &&
			((nw_dst.equals("") || p.nw_dst.equals("")) || (nw_dst.equals(p.nw_dst))) &&
			((nw_proto.equals("") || p.nw_proto.equals("")) || (nw_proto.equals(p.nw_proto))) &&
			((tp_src_port.equals("") || p.tp_src_port.equals("")) || (tp_src_port.equals(p.tp_src_port))) &&
			((tp_dst_port.equals("") || p.tp_dst_port.equals("")) || (tp_dst_port.equals(p.tp_dst_port))) &&
			((of_type == -1 || p.of_type == -1) || (of_type  == p.of_type))){
			return true;
		} else
			return false;
			
			
		
	}
}

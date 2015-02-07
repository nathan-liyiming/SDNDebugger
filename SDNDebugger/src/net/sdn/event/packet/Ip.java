package net.sdn.event.packet;

public class Ip {
	public String nw_src;
	public String nw_dst;
	public String nw_type;

	// choose one from three
	public Icmp icmp;
	public Tcp tcp;
	public Udp udp;
}

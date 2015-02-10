package net.sdn.event.packet;

public class Ethernet {
	public String dl_src;
	public String dl_dst;
	public PacketType dl_type;
	// choose one
	public Arp arp;
	public Ip ip;
}

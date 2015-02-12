package net.sdn.event.packet;

public class Ethernet {
	public String dl_src;
	public String dl_dst;
	public PacketType dl_type;
	// choose one
	public Arp arp;
	public Ip ip;

	public boolean equals(Object eth) {
		return this.dl_src.equalsIgnoreCase(((Ethernet) eth).dl_src)
				&& this.dl_dst.equalsIgnoreCase(((Ethernet) eth).dl_dst)
				&& this.dl_type.equals(((Ethernet) eth).dl_type)
				&& ((this.arp != null && this.arp.equals(((Ethernet) eth).arp) || (this.ip != null && this.ip
						.equals(((Ethernet) eth).ip))));
	}
}

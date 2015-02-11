package net.sdn.event.packet;

public class Ip {
	public String nw_src;
	public String nw_dst;
	public PacketType nw_type;

	// choose one from three
	public Icmp icmp;
	public Tcp tcp;
	public Udp udp;

	public boolean equals(Object ip) {
		return this.nw_src.equals(((Ip) ip).nw_src)
				&& this.nw_dst.equals(((Ip) ip).nw_dst)
				&& this.nw_type.equals(((Ip) ip).nw_type)
				&& ((this.icmp != null && this.icmp.equals(((Ip) ip).icmp))
						|| (this.tcp != null && this.tcp.equals(((Ip) ip).tcp)) || (this.udp != null && this.udp
						.equals(((Ip) ip).udp)));
	}
}

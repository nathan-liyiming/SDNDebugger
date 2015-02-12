package net.sdn.event.packet;

public class Ip {
	public String nw_src;
	public String nw_dst;
	public PacketType nw_proto;

	// choose one from three
	public Icmp icmp;
	public Tcp tcp;
	public Udp udp;

	public boolean equals(Object ip) {
		return this.nw_src.equalsIgnoreCase(((Ip) ip).nw_src)
				&& this.nw_dst.equalsIgnoreCase(((Ip) ip).nw_dst)
				&& this.nw_proto.equals(((Ip) ip).nw_proto)
				&& ((this.icmp != null && this.icmp.equals(((Ip) ip).icmp))
						|| (this.tcp != null && this.tcp.equals(((Ip) ip).tcp)) || (this.udp != null && this.udp
						.equals(((Ip) ip).udp)));
	}
}

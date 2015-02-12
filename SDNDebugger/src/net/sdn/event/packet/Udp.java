package net.sdn.event.packet;

import java.util.Arrays;

public class Udp {
	public String udp_src;
	public String udp_dst;
	public byte[] payload;

	public boolean equals(Object udp) {
		return this.udp_src.equals(((Udp) udp).udp_src)
				&& this.udp_dst.equals(((Udp) udp).udp_dst)
				&& Arrays.equals(this.payload, ((Udp) udp).payload);
	}
}

package net.sdn.event.packet;

import java.util.Arrays;

public class Udp {
	public String src_port;
	public String dst_port;
	public byte[] payload;

	public boolean equals(Object udp) {
		return this.src_port.equals(((Udp) udp).src_port)
				&& this.dst_port.equals(((Udp) udp).dst_port)
				&& Arrays.equals(this.payload, ((Udp) udp).payload);
	}
}

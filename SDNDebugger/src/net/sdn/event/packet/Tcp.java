package net.sdn.event.packet;

import java.util.Arrays;

public class Tcp {
	public String tcp_src;
	public String tcp_dst;
	public byte[] payload;
	public OFPacket of_packet;

	public boolean equals(Object tcp) {
		return this.tcp_src.equalsIgnoreCase(((Tcp) tcp).tcp_src)
				&& this.tcp_dst.equalsIgnoreCase(((Tcp) tcp).tcp_dst)
				&& ((this.payload != null && Arrays.equals(this.payload,
						((Tcp) tcp).payload)) || this.of_packet != null
						&& this.of_packet.equals(((Tcp) tcp).of_packet));
	}
}

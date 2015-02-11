package net.sdn.event.packet;

import java.util.Arrays;

public class Tcp {
	public String src_port;
	public String dst_port;
	public byte[] payload;
	public OFPacket of_packet;

	public boolean equals(Object tcp) {
		return this.src_port.equals(((Tcp) tcp).src_port)
				&& this.dst_port.equals(((Tcp) tcp).dst_port)
				&& ((this.payload != null && Arrays.equals(this.payload,
						((Tcp) tcp).payload)) || this.of_packet != null
						&& this.of_packet.equals(((Tcp) tcp).of_packet));
	}
}

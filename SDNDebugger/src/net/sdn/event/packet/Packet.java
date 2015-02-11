package net.sdn.event.packet;

public class Packet {
	public Ethernet eth;

	public boolean equals(Object pkt) {
		return this.eth.equals(((Packet) pkt).eth);
	}
}

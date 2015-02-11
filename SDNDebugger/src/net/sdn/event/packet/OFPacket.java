package net.sdn.event.packet;

public class OFPacket {
	public String type;
	public Packet packet;

	public String match;
	public String instruction;

	public boolean equals(Object of) {
		return this.type.equals(((OFPacket) of).type)
				&& this.packet.equals(((OFPacket) of).packet)
				&& this.match.equals(((OFPacket) of).match)
				&& this.instruction.equals(((OFPacket) of).instruction);
	}
}

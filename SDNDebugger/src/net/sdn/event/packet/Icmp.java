package net.sdn.event.packet;

public class Icmp {
	public String op;
	public int sequence;

	public boolean equals(Object icmp) {
		return this.op.equalsIgnoreCase(((Icmp) icmp).op)
				&& this.sequence == ((Icmp) icmp).sequence;
	}
}

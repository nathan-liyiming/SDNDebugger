package net.sdn.event.packet;

public class Icmp {
	public String op;

	public boolean equals(Object icmp) {
		return this.op.equalsIgnoreCase(((Icmp) icmp).op);
	}
}

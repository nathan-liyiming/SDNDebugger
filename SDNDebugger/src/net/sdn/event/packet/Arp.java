package net.sdn.event.packet;

public class Arp {
	public String sha;
	public String tha;
	public String op;
	public String spa;
	public String tpa;

	public boolean equals(Object arp) {
		return this.sha.equalsIgnoreCase(((Arp) arp).sha)
				&& this.tha.equalsIgnoreCase(((Arp) arp).tha)
				&& this.op.equalsIgnoreCase(((Arp) arp).op)
				&& this.spa.equalsIgnoreCase(((Arp) arp).spa)
				&& this.tpa.equalsIgnoreCase(((Arp) arp).tpa);

	}
}

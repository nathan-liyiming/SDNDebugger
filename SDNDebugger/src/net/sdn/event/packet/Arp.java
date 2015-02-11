package net.sdn.event.packet;

public class Arp {
	public String sha;
	public String tha;
	public String op;
	public String spa;
	public String tpa;

	public boolean equals(Object arp) {
		return this.sha.equals(((Arp) arp).sha)
				&& this.tha.equals(((Arp) arp).tha)
				&& this.op.equals(((Arp) arp).op)
				&& this.spa.equals(((Arp) arp).spa)
				&& this.tpa.equals(((Arp) arp).tpa);

	}
}

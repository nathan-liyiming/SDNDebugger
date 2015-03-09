package net.sdn.event;

import com.google.gson.Gson;

import net.sdn.event.packet.Packet;

public class NetworkEvent extends Event{	
	public static final int DEFAULT_PRIORITY = 0;
	public Packet pkt;
	public String interf;
	public String sw;
	public long timeStamp;
	public NetworkEventDirection direction;

	public NetworkEvent() {

	}

	public NetworkEvent(Packet p, String s, String i, NetworkEventDirection direct, long time) {
		pkt = p;
		sw = s;
		interf = i;
		direction = direct;
		timeStamp = time;
	}

	public NetworkEvent(Packet p, String s, String i) {
		pkt = p;
		sw = s;
		interf = i;
	}

	public boolean equals(Object e) {
		return this.sw.equals(((NetworkEvent) e).sw)
				&& this.pkt.equals(((NetworkEvent) e).pkt)
				&& ((NetworkEvent) e).interf.contains(this.interf);
	}

	public String toString() {
		return new Gson().toJson(this);
	}
}

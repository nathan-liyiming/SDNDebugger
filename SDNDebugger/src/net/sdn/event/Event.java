package net.sdn.event;

import java.util.ArrayList;
import java.util.List;

import net.sdn.event.packet.Packet;

public class Event {
	public static final int DEFAULT_PRIORITY = 0;
	public Packet pkt;
	public List<String> interf = new ArrayList<String>();
	public String sw;
	public long timeStamp;
	public int priority = DEFAULT_PRIORITY;
	public String direction = "";

	public Event() {

	}

	public Event(int pri, Packet p, String s, List<String> i) {
		pkt = p;
		sw = s;
		interf = i;
		priority = pri;
	}

	public Event(int pri, Packet p, String s, String i) {
		pkt = p;
		sw = s;
		interf.add(i);
		priority = pri;
	}

	public boolean equals(Object e) {
		return this.sw.equals(((Event) e).sw)
				&& this.pkt.equals(((Event) e).pkt);
	}
}

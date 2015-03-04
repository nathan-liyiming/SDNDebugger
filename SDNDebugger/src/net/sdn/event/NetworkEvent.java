package net.sdn.event;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import net.sdn.event.packet.Packet;
import net.sdn.event.util.NetworkEventDirection;

public class NetworkEvent extends Event{
	private EventType eventType = EventType.NetworkEvent;
	
	public static final int DEFAULT_PRIORITY = 0;
	public Packet pkt;
	public List<String> interf = new ArrayList<String>();
	public String sw;
	public long timeStamp;
	public int priority = DEFAULT_PRIORITY;
	public NetworkEventDirection direction;

	public NetworkEvent() {

	}

	public NetworkEvent(int pri, Packet p, String s, List<String> i, NetworkEventDirection direct, long time) {
		pkt = p;
		sw = s;
		interf = i;
		priority = pri;
		direction = direct;
		timeStamp = time;
	}

	public NetworkEvent(int pri, Packet p, String s, String i) {
		pkt = p;
		sw = s;
		interf.add(i);
		priority = pri;
	}

	public boolean equals(Object e) {
		return this.sw.equals(((NetworkEvent) e).sw)
				&& this.pkt.equals(((NetworkEvent) e).pkt)
				&& ((NetworkEvent) e).interf.contains(this.interf.get(0));
	}

	public String toString() {
		return new Gson().toJson(this);
	}
	
	public EventType getEventType(){
		return eventType;
	}
}

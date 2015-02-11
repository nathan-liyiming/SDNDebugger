package net.sdn.event;

import java.util.List;

import net.sdn.event.packet.Packet;

public class EventGenerator {
	
	public static Event generateEvent(int priority, Packet p, String s, List<String> ports){
		Event e = new Event(priority, p, s, ports);
		return e;
	}

}

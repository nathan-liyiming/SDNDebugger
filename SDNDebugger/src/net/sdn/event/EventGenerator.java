package net.sdn.event;

import java.util.ArrayList;
import java.util.List;

import net.sdn.event.packet.Packet;

public class EventGenerator {

	public static Event generateEvent(int priority, Packet p, String s,
			List<String> ports, String direct, long time) {
		return new Event(priority, p, s, ports, direct, time);
	}
	
	public static Event generateEvent(int priority, Packet p, String s,
			String ports, String direct, long time) {
		List<String> temp = new ArrayList<String>();
		temp.add(ports);
		return new Event(priority, p, s, temp, direct, time);
	}

}

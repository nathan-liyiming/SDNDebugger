package net.sdn.policy;

import net.sdn.event.packet.Packet;

public class Policy {
	
	public int priority;
	public Match match;
	public String action;
	
	public boolean isMatched(Packet p){
		return false;
	}
}

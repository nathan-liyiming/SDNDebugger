package net.sdn.event;

public abstract class Event{
	
	private EventType eventType;
	
	public Event(){
	}
	
	public EventType getEventType(){
		return eventType;
	}
}
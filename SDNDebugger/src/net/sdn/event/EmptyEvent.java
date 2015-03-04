package net.sdn.event;

public class EmptyEvent extends Event {	
	
	private EventType eventType = EventType.EmptyEvent;
	
	public EmptyEvent() {
		
	}
	
	public EventType getEventType(){
		return eventType;
	}
}
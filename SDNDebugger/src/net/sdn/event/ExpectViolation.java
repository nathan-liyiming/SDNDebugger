package net.sdn.event;

public class ExpectViolation extends Event {	
	private EventType eventType = EventType.ExpectViolation;
	
	public ExpectViolation() {
	}

	public String toString() {
		return "ExpectViolation";
	}
	
	public EventType getEventType(){
		return eventType;
	} 
}
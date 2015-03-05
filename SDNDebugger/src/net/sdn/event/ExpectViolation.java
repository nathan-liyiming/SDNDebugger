package net.sdn.event;

public class ExpectViolation extends Event {
	Event context;

	public ExpectViolation() {
		this.context = null;
	}
	public ExpectViolation(Event e) {
		this.context = e;
	}

	public String toString() {
		return "ExpectViolation";
	}
}
package net.sdn.event;

public class ExpectSuccess extends Event {
	Event context;

	public ExpectSuccess() {
		this.context = null;
	}
	public ExpectSuccess(Event e) {
		this.context = e;
	}

	public String toString() {
		return "ExpectSuccess";
	}
}
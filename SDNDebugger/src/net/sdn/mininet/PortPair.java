package net.sdn.mininet;

public class PortPair {
	private String left;
	private String right;
	
	public PortPair(String left, String right) {
		this.left = left;
		this.right = right;
	}
	
	public String getLeft() {
		return left;
	}
	
	public String getRight() {
		return right;
	}
}

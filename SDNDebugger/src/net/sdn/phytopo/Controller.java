package net.sdn.phytopo;

public class Controller extends Node {
	public String type = "C";
	private String id = "";
	private String nw_addr = "";
	private String port = "";
	
	public Controller(String id, String nw_addr, String port) {
		this.id = id;
		this.nw_addr = nw_addr;
		this.port = port;
	}
	
	public String getNwAddr() {
		return nw_addr;
	}
	
	public String getPort() {
		return port;
	}
	
	public String getId() {
		return id;
	}
	
	public String getType() {
		return type;
	}
}

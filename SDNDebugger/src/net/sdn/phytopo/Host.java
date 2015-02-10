package net.sdn.phytopo;

public class Host extends Node{
	public String type = "H";
	private String dl_addr = "";
	private String nw_addr = "";
	private String id = "";
	
	public Host(String id, String dl_addr, String nw_addr){
		this.id = id;
		this.dl_addr = dl_addr;
		this.nw_addr = nw_addr;
	}

	public String getType(){
		return type;
	}

	public String getDlAddr() {
		return dl_addr;
	}

	public String getNwAddr() {
		return nw_addr;
	}

	public String getId() {
		return id;
	}

}

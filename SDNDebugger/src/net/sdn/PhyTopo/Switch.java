package net.sdn.PhyTopo;

public class Switch extends Node{
	public String type = "S";
	private String id = "";
	private String dpid = "";
	
	public String getType(){
		return type;
	}
	
	public Switch(String id, String dpid){
		this.id = id;
		this.dpid = dpid;
	}
	
	public String getDpid(){
		return dpid;
	}
	
	public String getId() {
		return id;
	}
}

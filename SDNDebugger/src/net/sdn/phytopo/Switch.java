package net.sdn.phytopo;

import java.util.ArrayList;
import java.util.List;

public class Switch extends Node{
	
	private List<String> ports = new ArrayList<String>();
	private List<String> attachedHosts = new ArrayList<String>();
	
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
	
	public List<String> getAllPorts(){
		return ports;
	}
	
	public void addPort(String p){
		ports.add(p);
	}
	
	public List<String> getAllPortsExcept(String s){
		List<String> tempPorts = new ArrayList<String>(getAllPorts());
		tempPorts.remove(s);
		return tempPorts;
	}
	
	public void addAttachedHost(String h){
		attachedHosts.add(h);
	}
	
	public List<String> getAttachedHosts(){
		return attachedHosts;
	}
	
}
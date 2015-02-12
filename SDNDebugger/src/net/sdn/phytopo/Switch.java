package net.sdn.phytopo;

import java.util.ArrayList;
import java.util.List;

import net.sdn.policy.Policy;

public class Switch extends Node{
	
	private List<Policy> policies = new ArrayList<Policy>();
	private List<String> ports = new ArrayList<String>();
	
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
	
	public List<Policy> getPolicies(){
		return policies;
	}
	
	public void addPolicy(Policy p){
		for (int i = 0; i < policies.size(); i++){
			if (policies.get(i).priority >= p.priority){
				policies.add(i, p);
				return;
			}
		}
		policies.add(p);
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
}

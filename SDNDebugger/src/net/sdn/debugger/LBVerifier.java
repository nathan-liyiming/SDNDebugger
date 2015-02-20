package net.sdn.debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sdn.event.Event;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class LBVerifier extends Verifier{
	
	private Switch loadBalancer = null;
	private HashMap<String, Long> portMonitor = new HashMap<String, Long>();
	private List<String> inPorts;
	private List<String> outPorts;
	private int count;
	private final int ALARM_PACKET_NUMBER = 10000;
	
	public LBVerifier(Switch s, List<String> inPorts, List<String> outPorts){
		loadBalancer = s;
		this.inPorts = inPorts;
		this.outPorts = outPorts;
		for (String port : outPorts){
			portMonitor.put(port, (long) 0);
		}
	}
	
	@Override
	protected boolean checkEvents(Event e){
		for (String s : outPorts){
			System.out.println("Port: " + s + portMonitor.get(s));
		}
		return true;
	}

	@Override
	public void verify(Event event) {
		// TODO Auto-generated method stub
		if (event.direction.equalsIgnoreCase("in") && event.sw.equalsIgnoreCase(loadBalancer.getId())
				&& inPorts.contains(event.interf.get(0))) {
			if (count < ALARM_PACKET_NUMBER)
				count++;
			else {
				checkEvents(null);
				count = 0;
			}
			
			return;
		}
		
		if (event.direction.equalsIgnoreCase("out") && event.sw.equalsIgnoreCase(loadBalancer.getId())
				&& outPorts.contains(event.interf.get(0))){
			long temp = portMonitor.get(event.interf.get(0));
			portMonitor.put(event.interf.get(0), temp + event.pkt.eth.ip.tcp.payload.length);
			return;
		}
		
	}
	
	public static void main(String[] args){
		PhyTopo po = new PhyTopo(args[0]);
		List<String> inPorts = new ArrayList<String>();
		inPorts.add("eth0");
		List<String> outPorts = new ArrayList<String>();
		outPorts.add("eth1");
		outPorts.add("eth2");
		Verifier v = new LBVerifier(po.getSwitch("s1"), inPorts, outPorts);
		v.addInterestedEvents(PacketType.TCP);
		// v.addInterestedEvents(PacketType.UDP);
		v.addInterestedEvents(PacketType.ICMP);
		new Thread(v).start();
	}
}

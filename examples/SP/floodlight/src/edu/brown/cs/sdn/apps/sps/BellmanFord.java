package edu.brown.cs.sdn.apps.sps;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import edu.brown.cs.sdn.apps.util.Host;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.routing.Link;

public class BellmanFord {
    /**
     * Each instance stores the distance to one host and link to next switch
     * or that host.
     */
    public static class Distance 
    {
    	public long d;
    	public int dstPort;
    	
    	public Distance(long d, int dstPort)
    	{
    		this.d = d;
    		this.dstPort = dstPort;
    	}
    	
    	public void set(long d, int dstPort)
    	{
    		this.d = d;
    		this.dstPort = dstPort;
    	}
    }
    
    /**
     * Compute the shortest path for each switch to the given host.
     */
    public static Map<Long, Distance> bellmanFordCompute(Host host, Map<Long, IOFSwitch> switches, Collection<Link> links)
    {	
    	// <DPID, Distance>
    	Map<Long, Distance> distances = new HashMap<Long, Distance>();
    	// initial distances
    	for (Long dpid: switches.keySet())
    	{
    		distances.put(dpid, new Distance(Long.MAX_VALUE, -1));
    	}
    	
    	IOFSwitch connectionSwitch = host.getSwitch();
    	if (connectionSwitch == null) 
    	{
    		// doesn't occur because we always pass active host
    		return null;
    	}
    	
    	distances.get(connectionSwitch.getId()).set(1, host.getPort());
    	
    	for (int i = 0; i < switches.size() - 1; i++) 
    	{
    		for (Link link: links)
    		{
    			if (link.getDst() != 0) 
    			{
    				Distance dst = distances.get(link.getDst());
    				Distance src = distances.get(link.getSrc());
    				// avoid switch has been removed
    				if (dst != null && src != null && dst.d != Long.MAX_VALUE) 
    				{
    					if (dst.d + 1 < src.d) 
    					{
    						src.set(dst.d + 1, link.getSrcPort());
    					}
    				}
    			}
    		}
    	}
    	
    	// check negative-weight cycle exists
    	// not occur
    	
    	return distances;
    }
}

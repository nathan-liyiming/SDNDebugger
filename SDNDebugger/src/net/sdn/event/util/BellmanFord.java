package net.sdn.event.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sdn.phytopo.Host;
import net.sdn.phytopo.Link;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

public class BellmanFord {
	public static class Distance {
		public long d;
		public String outPort;
		public String inPort;
		public Switch inSw;

		public Distance(long d, String outPort, String inPort, Switch inSw) {
			this.d = d;
			this.outPort = outPort;
			this.inPort = inPort;
			this.inSw = inSw;
		}

		public void set(long d, String outPort, String inPort, Switch inSw) {
			this.d = d;
			this.outPort = outPort;
			this.inPort = inPort;
			this.inSw = inSw;
		}
	}

	public static class Pair {
		public String src;
		public String dst;

		public Pair(String src, String dst) {
			this.src = src;
			this.dst = dst;
		}
		
		public boolean equals(Object p) {
			return ((Pair)p).src.equals(this.src) && ((Pair)p).dst.equals(this.dst);
		}
		
		public int hashCode() {
			return this.src.hashCode() + this.dst.hashCode();
		}
	}

	public static class Triple {
		public Switch sw;
		public String interf;
		public String direction;

		public Triple(Switch sw, String interf, String direction) {
			this.sw = sw;
			this.interf = interf;
			this.direction = direction;
		}
	}

	public static HashMap<BellmanFord.Pair, List<Triple>> bellmanFordCompute(
			PhyTopo phyTopo) {
		HashMap<BellmanFord.Pair, List<Triple>> map = new HashMap<BellmanFord.Pair, List<Triple>>();
		for (Host host : phyTopo.getHosts().values()) {
			Map<String, Distance> distances = new HashMap<String, Distance>();
			for (Switch sw : phyTopo.getSwitches().values()) {
				distances.put(sw.getId(), new Distance(Long.MAX_VALUE, "", "",
						null));
			}

			Switch connectionSwitch = host.getSwitch();
			distances.get(connectionSwitch.getId()).set(1, host.getAttachedSwitchInterf(), "",
					null);

			for (int i = 0; i < phyTopo.getSwitches().size(); i++) {
				for (Link link : phyTopo.getLinks()) {
					if (link.left.getType().equalsIgnoreCase("s")
							&& link.right.getType().equalsIgnoreCase("s")) {
						// left -> right
						Distance dst = distances.get(((Switch) link.right)
								.getId());
						Distance src = distances.get(((Switch) link.left)
								.getId());
						if (dst.d != Long.MAX_VALUE) {
							if (dst.d + 1 < src.d) {
								src.set(dst.d + 1, link.left_interf,
										link.right_interf, (Switch) link.right);
							}
						}
						// right -> left
						dst = distances.get(((Switch) link.left).getId());
						src = distances.get(((Switch) link.right).getId());
						if (dst.d != Long.MAX_VALUE) {
							if (dst.d + 1 < src.d) {
								src.set(dst.d + 1, link.right_interf,
										link.left_interf, (Switch) link.left);
							}
						}
					}
				}
			}

			for (Host otherHost : phyTopo.getHosts().values()) {
				if (!otherHost.getId().equals(host.getId())) {
					List<Triple> list = new LinkedList<Triple>();

					// first switch connecting to host
					list.add(new Triple(otherHost.getSwitch(), otherHost
							.getAttachedSwitchInterf(), "in"));
					Distance dis = distances.get(otherHost.getSwitch().getId());
					list.add(new Triple(otherHost.getSwitch(), dis.outPort,
							"out"));

					// core switches
					while (dis.inSw != null) {
						list.add(new Triple(dis.inSw, dis.inPort, "in"));
						Switch inSw = dis.inSw;
						dis = distances.get(inSw.getId());
						list.add(new Triple(inSw, dis.outPort, "out"));
					}

					map.put(new Pair(otherHost.getNwAddr(), host.getNwAddr()),
							list);
				}
			}
		}

		return map;
	}
}
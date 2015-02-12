package net.sdn.policy;

import java.net.UnknownHostException;

import net.sdn.event.packet.Packet;
import net.sdn.event.util.CIDRUtils;

public class Policy {
	public int priority = 0;
	public int in_port = -1;
	public String nw_proto = "";
	public String dl_type = "";
	public String nw_src = "";
	public String nw_dst = "";
	public String dl_src = "";
	public String dl_dst = "";
	public String tp_src = "";
	public String tp_dst = "";
	public String actions = ""; // allow or deny

	public boolean isMatched(Packet p) {
		try {
			if ((dl_type.equals("") || (p.eth != null && dl_type
					.equalsIgnoreCase(p.eth.dl_type.name())))
					&& (dl_src.equals("") || (p.eth != null && dl_src
							.equals(p.eth.dl_src)))
					&& (dl_dst.equals("") || (p.eth != null && dl_dst
							.equals(p.eth.dl_dst)))
					&& (nw_proto.equals("") || (p.eth != null
							&& p.eth.ip != null && nw_proto
								.equals(p.eth.ip.nw_proto.name())))
					&& (nw_src.equals("") || (p.eth != null && p.eth.ip != null && new CIDRUtils(
							nw_src).isInRange(p.eth.ip.nw_src)))
					&& (nw_dst.equals("") || (p.eth != null && p.eth.ip != null && new CIDRUtils(
							nw_dst).isInRange(p.eth.ip.nw_dst)))
					&& (tp_src.equals("") || (p.eth != null && p.eth.ip != null
							&& p.eth.ip.tcp != null && tp_src
								.equals(p.eth.ip.tcp.tcp_src)))
					&& (tp_src.equals("") || (p.eth != null && p.eth.ip != null
							&& p.eth.ip.tcp != null && tp_dst
								.equals(p.eth.ip.tcp.tcp_dst)))) {
				return true;
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}

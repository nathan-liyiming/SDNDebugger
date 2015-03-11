package net.sdn.event.packet;

import net.sdn.event.NetworkEvent;

public class OFPacket {
	public OFPacketType of_type;
	public Packet packet;

	public MatchFields matchFields;
	public String instruction;

	public boolean equals(Object of) {
		return this.of_type == ((OFPacket) of).of_type
				&& this.packet.equals(((OFPacket) of).packet)
				&& this.matchFields.equals(((OFPacket) of).matchFields)
				&& this.instruction.equals(((OFPacket) of).instruction);
	}

	public boolean isMatch(NetworkEvent e) {
		if ((matchFields.int_port == 0 || (e.interf.startsWith("eth") && matchFields.int_port == Integer
				.parseInt(e.interf.substring(3))))
				&& (matchFields.dl_src == null || matchFields.dl_src
						.equals(e.pkt.eth.dl_src))
				&& (matchFields.dl_dst == null || matchFields.dl_dst
						.equals(e.pkt.eth.dl_dst))
				&& (matchFields.nw_src == null || (e.pkt.eth.ip != null && matchFields.nw_src
						.equals(e.pkt.eth.ip.nw_src)))
				&& (matchFields.nw_dst == null || (e.pkt.eth.ip != null && matchFields.nw_dst
						.equals(e.pkt.eth.ip.nw_dst)))
				&& (matchFields.tcp_src == null || (e.pkt.eth.ip != null
						&& e.pkt.eth.ip.tcp != null && matchFields.tcp_src
							.equals(e.pkt.eth.ip.tcp.tcp_src)))
				&& (matchFields.tcp_dst == null || (e.pkt.eth.ip != null
						&& e.pkt.eth.ip.tcp != null && matchFields.tcp_dst
							.equals(e.pkt.eth.ip.tcp.tcp_dst)))) {
			return true;
		}
		return false;
	}

	public static class MatchFields {
		public int int_port;
		public String dl_src;
		public String dl_dst;
		public String nw_src;
		public String nw_dst;
		public String tcp_src;
		public String tcp_dst;
	}

	public enum OFPacketType {
		ECHO_REPLY, ECHO_REQUEST, PACKET_IN, PACKET_OUT, FLOW_MOD
	}
}

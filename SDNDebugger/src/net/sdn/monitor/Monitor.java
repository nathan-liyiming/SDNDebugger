package net.sdn.monitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sdn.event.NetworkEventDirection;
import net.sdn.event.packet.OFPacket;
import net.sdn.event.packet.PacketType;
import net.sdn.phytopo.Controller;
import net.sdn.phytopo.Link;
import net.sdn.phytopo.PhyTopo;
import net.sdn.phytopo.Switch;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jnetpcap.Pcap;
import org.jnetpcap.Pcap.Direction;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.JMemoryPacket;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Icmp;
import org.jnetpcap.protocol.network.Icmp.IcmpType;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;

import com.google.gson.Gson;

public class Monitor {

	private Socket socket;
	private PhyTopo topo;
	private static String[] switches;
	private static String controller_port;
	private static int count = 0;
	private HashMap<String, String> port_sw = new HashMap<String, String>();

	// private OFFactory factory = OFFactory.getInstance();

	public Monitor(int port, PhyTopo topo) {
		try {
			socket = new Socket("127.0.0.1", port);
			this.topo = topo;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PhyTopo getPhyTopo() {
		return topo;
	}

	public Socket getSocket() {
		return socket;
	}

	/**
	 * Main startup method
	 * 
	 * @param args
	 *            ignored
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		Monitor monitor = new Monitor(8200, new PhyTopo(args[0]));
		OutputStream outputStream = null;
		try {
			outputStream = monitor.getSocket().getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PrintWriter out = new PrintWriter(outputStream);
		final RecordSorter rs = new RecordSorter(out);
		new Thread(rs).start();

		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with
														// NICs
		StringBuilder errbuf = new StringBuilder(); // For any error msgs

		int r = Pcap.findAllDevs(alldevs, errbuf);
		if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s",
					errbuf.toString());
			return;
		}

		switches = monitor.getPhyTopo().getSwitches().keySet()
				.toArray(new String[0]);
		controller_port = monitor.getPhyTopo().getControllers().values()
				.toArray(new Controller[0])[0].getPort();

		for (Link link : monitor.getPhyTopo().getLinks()) {
			if (link.left.getType().equals("S")) {
				monitor.capturePackets(((Switch) (link.left)).getId() + "-"
						+ link.left_interf, rs, Direction.IN);
				monitor.capturePackets(((Switch) (link.left)).getId() + "-"
						+ link.left_interf, rs, Direction.OUT);
			}
			if (link.right.getType().equals("S")) {
				monitor.capturePackets(((Switch) (link.right)).getId() + "-"
						+ link.right_interf, rs, Direction.IN);
				monitor.capturePackets(((Switch) (link.right)).getId() + "-"
						+ link.right_interf, rs, Direction.OUT);
			}
		}

		// openflow message
		monitor.capturePackets("lo", rs, Direction.INOUT);
	}

	public void capturePackets(final String interf, final RecordSorter rs,
			final Direction direction) {
		StringBuilder errbuf = new StringBuilder(); // For any error msgs

		int snaplen = 64 * 1024; // Capture all packets, no trucation
		int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
		int timeout = 1; // 1 milli

		final Pcap pcap = Pcap
				.openLive(interf, snaplen, flags, timeout, errbuf);

		if (pcap == null) {
			System.err.printf("Error while opening device for capture: "
					+ errbuf.toString());
			return;
		}

		pcap.setDirection(direction);

		if (interf.equals("lo")) {
			PcapBpfProgram filter = new PcapBpfProgram();
			String expr = "";
			for (Controller controller : this.topo.getControllers().values()) {
				expr += "dst port " + controller.getPort();
				expr += " or ";
				expr += "src port " + controller.getPort();
			}

			if (pcap.compile(filter, expr, 0, 0) != Pcap.OK) {
				System.err.println(pcap.getErr());
				return;
			}

			if (pcap.setFilter(filter) != Pcap.OK) {
				System.err.println(pcap.getErr());
				return;
			}
		}

		final PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

			public void nextPacket(PcapPacket jpacket, String interf) {
				Ethernet eth = new Ethernet();
				Arp arp = new Arp();
				Ip4 ip = new Ip4();
				Icmp icmp = new Icmp();
				Tcp tcp = new Tcp();
				Udp udp = new Udp();

				// simplified packets objects
				net.sdn.event.NetworkEvent sEvt = new net.sdn.event.NetworkEvent();
				net.sdn.event.packet.Packet sPkt = new net.sdn.event.packet.Packet();
				net.sdn.event.packet.Ethernet sEth = new net.sdn.event.packet.Ethernet();
				net.sdn.event.packet.Arp sArp = new net.sdn.event.packet.Arp();
				net.sdn.event.packet.Ip sIp = new net.sdn.event.packet.Ip();
				net.sdn.event.packet.Icmp sIcmp = new net.sdn.event.packet.Icmp();
				net.sdn.event.packet.Tcp sTcp = new net.sdn.event.packet.Tcp();
				net.sdn.event.packet.OFPacket sOf = new net.sdn.event.packet.OFPacket();
				net.sdn.event.packet.Udp sUdp = new net.sdn.event.packet.Udp();

				// eth
				sEvt.timeStamp = jpacket.getCaptureHeader().timestampInNanos();
				if (direction == Direction.IN) {
					sEvt.direction = NetworkEventDirection.IN;
				} else if (direction == Direction.OUT) {
					sEvt.direction = NetworkEventDirection.OUT;
				} else {
					sEvt.direction = NetworkEventDirection.CONTROLLER;
				}
				if (jpacket.hasHeader(eth)) {
					sEvt.pkt = sPkt;
					sPkt.eth = sEth;

					sEth.dl_src = FormatUtils.mac(eth.source());
					sEth.dl_dst = FormatUtils.mac(eth.destination());

					// arp
					if (jpacket.hasHeader(arp)) {
						sEth.dl_type = PacketType.ARP;
						sEth.arp = sArp;
						sArp.sha = FormatUtils.mac(arp.sha());
						sArp.tha = FormatUtils.mac(arp.tha());
						if (arp.operationEnum() == Arp.OpCode.REQUEST) {
							sArp.op = "request";
						} else if (arp.operationEnum() == Arp.OpCode.REPLY) {
							sArp.op = "reply";
						}
						// IPv4
					} else if (jpacket.hasHeader(ip)) {
						sEth.dl_type = PacketType.IP;
						sEth.ip = sIp;
						sIp.nw_src = FormatUtils.ip(ip.source());
						sIp.nw_dst = FormatUtils.ip(ip.destination());

						// icmp
						if (jpacket.hasHeader(icmp)) {
							sIp.nw_proto = PacketType.ICMP;
							sIp.icmp = sIcmp;
							if (icmp.typeEnum() == IcmpType.ECHO_REQUEST) {
								Icmp.EchoRequest echo = new Icmp.EchoRequest();
								icmp.hasSubHeader(echo);
								sIcmp.op = "request";
								sIcmp.sequence = echo.sequence();
							} else if (icmp.typeEnum() == IcmpType.ECHO_REPLY) {
								Icmp.EchoReply echo = new Icmp.EchoReply();
								icmp.hasSubHeader(echo);
								sIcmp.op = "reply";
								sIcmp.sequence = echo.sequence();
							}
							// tcp
						} else if (jpacket.hasHeader(tcp)) {
							sIp.nw_proto = PacketType.TCP;
							sIp.tcp = sTcp;
							sTcp.tcp_src = new Integer(tcp.source()).toString();
							sTcp.tcp_dst = new Integer(tcp.destination())
									.toString();
							if (!interf.equals("lo")) {
								if (tcp.source() == 0 || tcp.destination() == 0) {
									return;
								}
								sTcp.payload = tcp.getPayload();
							} else {
								sTcp.of_packet = sOf;

								// ACK from controller, no payload
								if (tcp.getPayload().length == 0) {
									return;
								}

								OFMessage message = null;
								try {
									message = OFFactories
											.getGenericReader()
											.readFrom(
													ChannelBuffers.copiedBuffer(tcp
															.getPayload()));
									// first two heart beat
									if (message.getType() == OFType.ECHO_REPLY) {
										sOf.of_type = OFPacket.OFPacketType.ECHO_REPLY;
									} else if (message.getType() == OFType.ECHO_REQUEST) {
										sOf.of_type = OFPacket.OFPacketType.ECHO_REQUEST;
									} else if (message.getType() == OFType.PACKET_IN) {
										sOf.of_type = OFPacket.OFPacketType.PACKET_IN;
										if (((OFPacketIn) message).getData().length == 0) {
											return;
										}
										JPacket innerPacket = new JMemoryPacket(
												Ethernet.ID,
												((OFPacketIn) message)
														.getData());
										sOf.packet = generateInnnerPacket(innerPacket);
										if (sOf.packet == null) {
											return;
										}
									} else if (message.getType() == OFType.PACKET_OUT) {
										sOf.of_type = OFPacket.OFPacketType.PACKET_OUT;
										JPacket innerPacket = new JMemoryPacket(
												Ethernet.ID,
												((OFPacketOut) message)
														.getData());
										sOf.packet = generateInnnerPacket(innerPacket);
									} else if (message.getType() == OFType.FLOW_MOD) {
										sOf.of_type = OFPacket.OFPacketType.FLOW_MOD;
										OFPacket.MatchFields smatch = new OFPacket.MatchFields();
										Match match = ((OFFlowMod) message).getMatch();
										// set all the fields
										try {
											smatch.int_port = match.get(MatchField.IN_PORT).getPortNumber();
										} catch (Exception e) {
											// if it has no port set
										}
										try {
											smatch.dl_src = match.get(MatchField.ETH_SRC).toString();
											smatch.dl_dst = match.get(MatchField.ETH_DST).toString();
											smatch.nw_src = match.get(MatchField.IPV4_SRC).toString();
											smatch.nw_dst = match.get(MatchField.IPV4_DST).toString();
											smatch.tcp_src = match.get(MatchField.TCP_SRC).toString();
											smatch.tcp_dst = match.get(MatchField.TCP_DST).toString();
										} catch (Exception e) {
											// from low to high
										}
										sOf.matchFields = smatch;
										if (message.getVersion() == OFVersion.OF_13) {
											Gson gson = new Gson();
											sOf.instruction = gson
													.toJson((((OFFlowMod) message)
															.getInstructions()));
										} else {
											sOf.instruction = ((OFFlowMod) message)
													.getActions().toString();
										}

									}else {
										return;
									}
								} catch (OFParseError e) {
									e.printStackTrace();
								}
							}
							// udp
						} else if (jpacket.hasHeader(udp)) {
							sIp.nw_proto = PacketType.UDP;
							sIp.udp = sUdp;
							sUdp.udp_src = new Integer(udp.source()).toString();
							sUdp.udp_dst = new Integer(udp.destination())
									.toString();
							sUdp.payload = udp.getPayload();
						} else {
							return;
						}
					} else {
						return;
					}

					if (!interf.equals("lo")) {
						sEvt.sw = interf.split("-")[0];
						sEvt.interf = interf.split("-")[1];
					} else {
						String sw_port = "";
						if (!sTcp.tcp_src.equals(controller_port)) {
							sw_port = sTcp.tcp_src;
						} else {
							sw_port = sTcp.tcp_dst;
						}

						if (!port_sw.containsKey(sw_port)) {
							port_sw.put(sw_port, switches[count++]);
						}
						sEvt.sw = port_sw.get(sw_port);
					}

					rs.insertRecord(sEvt.timeStamp, sEvt);
				}
			}
		};

		new Thread() {
			public void run() {
				pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, interf);
				pcap.close();
			}
		}.start();
	}

	public net.sdn.event.packet.Packet generateInnnerPacket(JPacket jpacket) {
		Ethernet eth = new Ethernet();
		Arp arp = new Arp();
		Ip4 ip = new Ip4();
		Icmp icmp = new Icmp();
		Tcp tcp = new Tcp();
		Udp udp = new Udp();

		// simplified packets objects
		net.sdn.event.packet.Packet sPkt = new net.sdn.event.packet.Packet();
		net.sdn.event.packet.Ethernet sEth = new net.sdn.event.packet.Ethernet();
		net.sdn.event.packet.Arp sArp = new net.sdn.event.packet.Arp();
		net.sdn.event.packet.Ip sIp = new net.sdn.event.packet.Ip();
		net.sdn.event.packet.Icmp sIcmp = new net.sdn.event.packet.Icmp();
		net.sdn.event.packet.Tcp sTcp = new net.sdn.event.packet.Tcp();
		net.sdn.event.packet.Udp sUdp = new net.sdn.event.packet.Udp();

		// eth
		if (jpacket.hasHeader(eth)) {
			sPkt.eth = sEth;

			sEth.dl_src = FormatUtils.mac(eth.source());
			sEth.dl_dst = FormatUtils.mac(eth.destination());

			// arp
			if (jpacket.hasHeader(arp)) {
				sEth.dl_type = PacketType.ARP;
				sEth.arp = sArp;
				sArp.sha = FormatUtils.mac(arp.sha());
				sArp.tha = FormatUtils.mac(arp.tha());
				if (arp.operationEnum() == Arp.OpCode.REQUEST) {
					sArp.op = "request";
				} else if (arp.operationEnum() == Arp.OpCode.REPLY) {
					sArp.op = "reply";
				}
				// IPv4
			} else if (jpacket.hasHeader(ip)) {
				sEth.dl_type = PacketType.IP;
				sEth.ip = sIp;
				sIp.nw_src = FormatUtils.ip(ip.source());
				sIp.nw_dst = FormatUtils.ip(ip.destination());

				// icmp
				if (jpacket.hasHeader(icmp)) {
					sIp.nw_proto = PacketType.ICMP;
					sIp.icmp = sIcmp;
					if (icmp.typeEnum() == IcmpType.ECHO_REQUEST) {
						Icmp.EchoRequest echo = new Icmp.EchoRequest();
						icmp.hasSubHeader(echo);
						sIcmp.op = "request";
						sIcmp.sequence = echo.sequence();
					} else if (icmp.typeEnum() == IcmpType.ECHO_REPLY) {
						Icmp.EchoReply echo = new Icmp.EchoReply();
						icmp.hasSubHeader(echo);
						sIcmp.op = "reply";
						sIcmp.sequence = echo.sequence();
					}
					// tcp
				} else if (jpacket.hasHeader(tcp)) {
					sIp.nw_proto = PacketType.TCP;
					sIp.tcp = sTcp;
					sTcp.tcp_src = new Integer(tcp.source()).toString();
					sTcp.tcp_dst = new Integer(tcp.destination()).toString();

					sTcp.payload = tcp.getPayload();
					// udp
				} else if (jpacket.hasHeader(udp)) {
					sIp.nw_proto = PacketType.UDP;
					sIp.udp = sUdp;
					sUdp.udp_src = new Integer(udp.source()).toString();
					sUdp.udp_dst = new Integer(udp.destination()).toString();
					sUdp.payload = udp.getPayload();
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
		return sPkt;
	}
}
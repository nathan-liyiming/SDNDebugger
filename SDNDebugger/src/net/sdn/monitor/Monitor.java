package net.sdn.monitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.sdn.debugger.Debugger.*;
import net.sdn.PhyTopo.Controller;
import net.sdn.PhyTopo.PhyTopo;
import net.sdn.PhyTopo.Link;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;
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
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;

import com.google.gson.Gson;

public class Monitor {

	private Socket socket;
	private PhyTopo topo;
	private static String[] switches;
	private static String controller_port;
	private static int count = 0;
	private HashMap<String, String> port_sw = new HashMap<String, String>();

	private BasicFactory factory = BasicFactory.getInstance();

	public Monitor(int port, PhyTopo topo) {
		try {
			socket = new Socket("127.0.0.1", port);
			this.topo = topo;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	public static void main(String[] args) {
		Monitor monitor = new Monitor(DEFAULT_MONITOR_PORT,
				new PhyTopo(args[0]));
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
			if (link.left_interf.contains("s")) {
				monitor.generateHandler(link.left_interf, rs);
			}
			if (link.right_interf.contains("s")) {
				monitor.generateHandler(link.right_interf, rs);
			}
		}

		// openflow message
		monitor.generateHandler("lo", rs);
	}

	public void generateHandler(final String interf, final RecordSorter rs) {
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
				net.sdn.event.Event sEvt = new net.sdn.event.Event();
				net.sdn.event.packet.Packet sPkt = new net.sdn.event.packet.Packet();
				net.sdn.event.packet.Ethernet sEth = new net.sdn.event.packet.Ethernet();
				net.sdn.event.packet.Arp sArp = new net.sdn.event.packet.Arp();
				net.sdn.event.packet.Ip sIp = new net.sdn.event.packet.Ip();
				net.sdn.event.packet.Icmp sIcmp = new net.sdn.event.packet.Icmp();
				net.sdn.event.packet.Tcp sTcp = new net.sdn.event.packet.Tcp();
				net.sdn.event.packet.OFPacket sOf = new net.sdn.event.packet.OFPacket();
				net.sdn.event.packet.Udp sUdp = new net.sdn.event.packet.Udp();

				// eth
				if (jpacket.hasHeader(eth)) {
					sEvt.pkt = sPkt;
					sPkt.eth = sEth;

					sEth.timeStamp = jpacket.getCaptureHeader()
							.timestampInNanos();
					sEth.dl_src = FormatUtils.mac(eth.source());
					sEth.dl_dst = FormatUtils.mac(eth.destination());

					// arp
					if (jpacket.hasHeader(arp)) {
						sEth.dl_type = "arp";
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
						sEth.dl_type = "ip";
						sEth.ip = sIp;
						sIp.nw_src = FormatUtils.ip(ip.source());
						sIp.nw_dst = FormatUtils.ip(ip.destination());

						// icmp
						if (jpacket.hasHeader(icmp)) {
							sIp.nw_type = "icmp";
							sIp.icmp = sIcmp;
							if (icmp.typeEnum() == IcmpType.ECHO_REQUEST) {
								sIcmp.op = "request";
							} else if (icmp.typeEnum() == IcmpType.ECHO_REPLY) {
								sIcmp.op = "reply";
							}
							// tcp
						} else if (jpacket.hasHeader(tcp)) {
							sIp.nw_type = "tcp";
							sIp.tcp = sTcp;
							sTcp.src_port = new Integer(tcp.source())
									.toString();
							sTcp.dst_port = new Integer(tcp.destination())
									.toString();
							if (!interf.equals("lo")) {
								sTcp.payload = tcp.getPayload();
							} else {
								sTcp.of_packet = sOf;

								// ACK from controller, no payload
								if (tcp.getPayload().length == 0) {
									return;
								}

								ByteBuffer buf = ByteBuffer.wrap(tcp
										.getPayload());
								List<OFMessage> l = factory.parseMessages(buf);
								OFMessage message = l.get(0);
								// first two heart beat
								if (message.getType() == OFType.ECHO_REPLY) {
									sOf.type = "echo_reply";
								} else if (message.getType() == OFType.ECHO_REQUEST) {
									sOf.type = "echo_request";
								} else if (message.getType() == OFType.PACKET_IN) {
									sOf.type = "packet_in";
									JPacket innerPacket = new JMemoryPacket(Ethernet.ID,
											((OFPacketIn) message)
											.getPacketData());
									sOf.packet = generateInnnerPacket(innerPacket);
								} else if (message.getType() == OFType.PACKET_OUT) {
									sOf.type = "packet_out";
									JPacket innerPacket = new JMemoryPacket(Ethernet.ID,
											((OFPacketOut) message)
											.getPacketData());
									sOf.packet = generateInnnerPacket(innerPacket);
								} else if (message.getType() == OFType.FLOW_MOD) {
									sOf.type = "flow_mod";
									sOf.match = ((OFFlowMod) message)
											.getMatch().getMatchFields()
											.toString();
									sOf.instruction = ((OFFlowMod) message)
											.getInstructions().toString();
								} /*
								 * else if (message.getType() ==
								 * OFType.FLOW_REMOVED) { sOf.type =
								 * "flow_removed"; sOf.match = ((OFFlowRemoved)
								 * message
								 * ).getMatch().getMatchFields().toString();
								 * sOf.instruction = ((OFFlowMod)
								 * message).getInstructions().toString(); }
								 */else {
									return;
								}
							}
							// udp
						} else if (jpacket.hasHeader(udp)) {
							sIp.nw_type = "udp";
							sIp.udp = sUdp;
							sUdp.src_port = new Integer(udp.source())
									.toString();
							sUdp.dst_port = new Integer(udp.destination())
									.toString();
							sUdp.payload = udp.getPayload();
						}
					}

					if (!interf.equals("lo")) {
						sEvt.sw = interf.split("-")[0];
						sEvt.interf = interf.split("-")[1];
					} else {
						String sw_port = "";
						if (!sTcp.src_port.equals(controller_port)) {
							sw_port = sTcp.src_port;
						} else {
							sw_port = sTcp.dst_port;
						}

						if (!port_sw.containsKey(sw_port)) {
							port_sw.put(sw_port, switches[count++]);
						}
						sEvt.sw = port_sw.get(sw_port);
					}

					// serialization
					Gson gson = new Gson();
					String json = gson.toJson(sEvt);
					System.out.println(json);
					rs.insetRecord(sEth.timeStamp, json);
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

			sEth.timeStamp = jpacket.getCaptureHeader().timestampInNanos();
			sEth.dl_src = FormatUtils.mac(eth.source());
			sEth.dl_dst = FormatUtils.mac(eth.destination());

			// arp
			if (jpacket.hasHeader(arp)) {
				sEth.dl_type = "arp";
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
				sEth.dl_type = "ip";
				sEth.ip = sIp;
				sIp.nw_src = FormatUtils.ip(ip.source());
				sIp.nw_dst = FormatUtils.ip(ip.destination());

				// icmp
				if (jpacket.hasHeader(icmp)) {
					sIp.nw_type = "icmp";
					sIp.icmp = sIcmp;
					if (icmp.typeEnum() == IcmpType.ECHO_REQUEST) {
						sIcmp.op = "request";
					} else if (icmp.typeEnum() == IcmpType.ECHO_REPLY) {
						sIcmp.op = "reply";
					}
					// tcp
				} else if (jpacket.hasHeader(tcp)) {
					sIp.nw_type = "tcp";
					sIp.tcp = sTcp;
					sTcp.src_port = new Integer(tcp.source()).toString();
					sTcp.dst_port = new Integer(tcp.destination()).toString();

					sTcp.payload = tcp.getPayload();
					// udp
				} else if (jpacket.hasHeader(udp)) {
					sIp.nw_type = "udp";
					sIp.udp = sUdp;
					sUdp.src_port = new Integer(udp.source()).toString();
					sUdp.dst_port = new Integer(udp.destination()).toString();
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
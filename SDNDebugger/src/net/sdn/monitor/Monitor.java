package net.sdn.monitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static net.sdn.debugger.Debugger.*;
import net.sdn.PhyTopo.Controller;
import net.sdn.PhyTopo.PhyTopo;
import net.sdn.PhyTopo.Link;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Icmp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

public class Monitor {

	private Socket socket;
	private PhyTopo topo;

	private static Integer lock = new Integer(0);

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

		final PrintWriter out = new PrintWriter(outputStream);

		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with
														// NICs
		StringBuilder errbuf = new StringBuilder(); // For any error msgs

		/***************************************************************************
		 * First get a list of devices on this system
		 **************************************************************************/
		int r = Pcap.findAllDevs(alldevs, errbuf);
		if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s",
					errbuf.toString());
			return;
		}

		for (Link link : monitor.getPhyTopo().getLinks()) {
			if (link.left_interf.contains("s")) {
				monitor.generateHandler(link.left_interf, out);
			}
			if (link.right_interf.contains("s")) {
				monitor.generateHandler(link.right_interf, out);
			}
		}

		// openflow message
		monitor.generateHandler("lo", out);
	}

	public void generateHandler(final String interf, final PrintWriter out) {
		StringBuilder errbuf = new StringBuilder(); // For any error msgs

		/***************************************************************************
		 * Second we open up the selected device
		 **************************************************************************/
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

		/***************************************************************************
		 * Third we create a packet handler which will receive packets from the
		 * libpcap loop.
		 **************************************************************************/
		final PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

			public void nextPacket(PcapPacket packet, String interf) {
//				 timestamp
//				 eth type
//				 dl src
//				 dl dst
//				 dl op
//				 nw type
//				 nw src
//				 nw dst
//				 src port
//				 dst port
//				 payload
				
				 String timestamp = new Long(packet.getCaptureHeader().timestampInNanos()).toString();
				 String eth_type = "";
				 String dl_src = "";
				 String dl_dst = "";
				 String dl_op = "";
				 String nw_type = "";
				 String nw_src = "";
				 String nw_dst = "";
				 String nw_op = "";
				 String src_port = "";
				 String dst_port = "";
				 String payload = "";
				
				 Ethernet eth = new Ethernet();
				 Arp arp = new Arp();
				 Icmp icmp = new Icmp();
				 Ip4 ip = new Ip4();
				 Tcp tcp = new Tcp();
				 Udp udp = new Udp();
				 
				 // eth
				 if (packet.hasHeader(eth)) {
					 dl_src = FormatUtils.mac(eth.destination());
					 dl_src = FormatUtils.mac(eth.source());
					 
					// arp
					 if (packet.hasHeader(arp)) {
						 eth_type = "arp";
						 dl_src = arp.sha().toString();
						 dl_dst = arp.tha().toString();
						 if (arp.operationEnum() == Arp.OpCode.REQUEST){
							dl_op = "request";
						 } else if(arp.operationEnum() == Arp.OpCode.REPLY){
							dl_op = "reply";
						 }
						 // IPv4
					 } else if (packet.hasHeader(ip)){
						 
					 }
						 
					 
				 }
				 
				
				 
				// icmp
				 Icmp icmp = new Icmp();
				 if (packet.hasHeader(icmp)){
					 eth_type = "ip";
					 dl_src = icmp.get
				 }
				
				// tcp/udp
				
				// of message
				synchronized (lock) {
					out.println(packet.getCaptureHeader().timestampInNanos() + "");
					out.flush();
				}

			}
		};

		/***************************************************************************
		 * Fourth we enter the loop and tell it to capture 10 packets. The loop
		 * method does a mapping of pcap.datalink() DLT value to JProtocol ID,
		 * which is needed by JScanner. The scanner scans the packet buffer and
		 * decodes the headers. The mapping is done automatically, although a
		 * variation on the loop method exists that allows the programmer to
		 * sepecify exactly which protocol ID to use as the data link type for
		 * this pcap interface.
		 **************************************************************************/
		new Thread() {
			public void run() {
				pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, interf);

				/***************************************************************************
				 * Last thing to do is close the pcap handle
				 **************************************************************************/
				pcap.close();
			}
		}.start();
	}
}
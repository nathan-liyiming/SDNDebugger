package net.sdn.monitor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import net.sdn.event.packet.PacketType;

import org.jnetpcap.Pcap;
import org.jnetpcap.Pcap.Direction;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;

import com.google.gson.Gson;

public class RESTMonitor {

	private Socket socket;

	public RESTMonitor(int port) {
		try {
			socket = new Socket("127.0.0.1", port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		RESTMonitor monitor = new RESTMonitor(8200);
		OutputStream outputStream = null;
		try {
			outputStream = monitor.getSocket().getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PrintWriter out = new PrintWriter(outputStream);

		List<PcapIf> alldevs = new ArrayList<PcapIf>(); // Will be filled with
														// NICs
		StringBuilder errbuf = new StringBuilder(); // For any error msgs

		int r = Pcap.findAllDevs(alldevs, errbuf);
		if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s",
					errbuf.toString());
			return;
		}

		// openflow message
		monitor.capturePackets("lo", Direction.INOUT, out);
	}

	public void capturePackets(final String interf, final Direction direction,
			final PrintWriter out) {
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

		PcapBpfProgram filter = new PcapBpfProgram();
		String expr = "dst port 8080";

		if (pcap.compile(filter, expr, 0, 0) != Pcap.OK) {
			System.err.println(pcap.getErr());
			return;
		}

		if (pcap.setFilter(filter) != Pcap.OK) {
			System.err.println(pcap.getErr());
			return;
		}

		final PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {

			public void nextPacket(PcapPacket jpacket, String interf) {
				Ethernet eth = new Ethernet();
				Ip4 ip = new Ip4();
				Tcp tcp = new Tcp();
				Http http = new Http();

				// simplified packets objects
				net.sdn.event.Event sEvt = new net.sdn.event.Event();
				net.sdn.event.packet.Packet sPkt = new net.sdn.event.packet.Packet();
				net.sdn.event.packet.Ethernet sEth = new net.sdn.event.packet.Ethernet();
				net.sdn.event.packet.Ip sIp = new net.sdn.event.packet.Ip();
				net.sdn.event.packet.Tcp sTcp = new net.sdn.event.packet.Tcp();

				// eth
				sEvt.timeStamp = jpacket.getCaptureHeader().timestampInNanos();
				if (jpacket.hasHeader(eth)) {
					sEvt.pkt = sPkt;
					sPkt.eth = sEth;

					sEth.dl_src = FormatUtils.mac(eth.source());
					sEth.dl_dst = FormatUtils.mac(eth.destination());

					// IPv4
					if (jpacket.hasHeader(ip)) {
						sEth.dl_type = PacketType.IP;
						sEth.ip = sIp;
						sIp.nw_src = FormatUtils.ip(ip.source());
						sIp.nw_dst = FormatUtils.ip(ip.destination());

						// tcp
						if (jpacket.hasHeader(tcp)) {
							sIp.nw_proto = PacketType.TCP;
							sIp.tcp = sTcp;
							sTcp.tcp_src = new Integer(tcp.source()).toString();
							sTcp.tcp_dst = new Integer(tcp.destination())
									.toString();
							if (jpacket.hasHeader(http)) {
								if (http.getPayload().length == 0)
									return;
								else
									sTcp.payload = http.getPayload();
							} else {
								return;
							}
						} else {
							return;
						}
					} else {
						return;
					}
				} else {
					return;
				}

				// serialization
				Gson gson = new Gson();
				String json = gson.toJson(sEvt);
				System.out.println(json);
				out.println(json);
				out.flush();
			}
		};

		new Thread() {
			public void run() {
				pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, interf);
				pcap.close();
			}
		}.start();
	}
}
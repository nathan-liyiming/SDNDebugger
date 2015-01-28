package net.sdn.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import static net.sdn.debugger.Debugger.*;

public class Monitor {
	private static String ports[] = { "s1-eth1", "s1-eth2" };
	private static String hosts[] = { "s1" };
	private static int count = 0;
	private static boolean flag = true;
	private static HashMap<Integer, String> hostsMap = new HashMap<Integer, String>();

	private Socket socket;

	public Monitor(int port) {
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

	private static String[] generateParams() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("/usr/bin/sudo");
		list.add("/usr/local/bin/tshark");
		list.add("-l");

		// listen on each port of each switch
		for (String port : ports) {
			list.add("-i");
			list.add(port);
		}
		// openflow message
		list.add("-i");
		list.add("lo");

		list.add("-T");
		list.add("fields");

		// layer 2
		list.add("-e");
		list.add("eth.src");
		list.add("-e");
		list.add("eth.dst");

		// layer 3
		list.add("-e");
		list.add("ip.src");
		list.add("-e");
		list.add("ip.dst");

		// layer 4
		list.add("-e");
		list.add("tcp.port");

		// types
		list.add("-e");
		list.add("frame.protocols");
		list.add("-e");
		list.add("openflow_v4.type");

		// which interface
		list.add("-e");
		list.add("frame.interface_id");

		list.add("-Y");
		list.add("ip || arp || openflow_v4");
		
		list.add("-e");
		list.add("frame.time_relative");

		String[] t = new String[1];
		return list.toArray(t);
	}

	public static void main(String args[]) {
		String[] params = generateParams();
		String line = null;
		try {
			OutputStream outputStream = new Monitor(DEFAULT_MONITOR_PORT)
					.getSocket().getOutputStream();
			PrintWriter out = new PrintWriter(outputStream);

			System.out.println("Start the monitor.");
			Process proc = Runtime.getRuntime().exec(params);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			
			RecordSorter t = new RecordSorter(out);
			t.start();

			while ((line = stdInput.readLine()) != null) {
				// 8 entries, if it doesn't contain, it will be empty string
				// 0. eth.src
				// 1. eth.dst
				// 2. ip.src
				// 3. ip.dst
				// 4. tcp.port
				// 5. frame.protocols
				// 6. openflow_v4.type
				// 7. frame.interface_id => interface/switch
				String array[] = line.split("\t");

				// filter here
				String interf = "";
				if (Integer.parseInt(array[7]) == ports.length) {
					// lo, should have of message
					if (array[5].contains("openflow")) {
						String tcpPort[] = array[4].split(",");
						if (flag) {
							// assume that host added as order
							if (Integer.parseInt(tcpPort[0]) != 6633) {
								if (!hostsMap.containsKey(Integer
										.parseInt(tcpPort[0]))) {
									hostsMap.put(Integer.parseInt(tcpPort[0]),
											hosts[count++]);
									if (count == hosts.length) {
										flag = false;
									}
								}
							}
						} 
						
						// get the interface
						if (Integer.parseInt(tcpPort[0]) != 6633) {
							interf = hostsMap.get(Integer.parseInt(tcpPort[0]));
						} else {
							interf = hostsMap.get(Integer.parseInt(tcpPort[1]));
						}
					} else {
						continue;
					}
				} else {
					interf = ports[Integer.parseInt(array[7])];
				}

				System.out.println(array[0] + "\t" + array[1] + "\t" + array[2] + "\t"
						+ array[3] + "\t" + array[4] + "\t" + array[5] + "\t"
						+ array[6] + "\t" + interf + "\t" + array[8]);
				
				t.insetRecord(Double.parseDouble(array[8]), array[0] + "\t" + array[1] + "\t" + array[2] + "\t"
						+ array[3] + "\t" + array[4] + "\t" + array[5] + "\t"
						+ array[6] + "\t" + interf + "\t" + array[8]);
//				out.println(array[0] + "\t" + array[1] + "\t" + array[2] + "\t"
//						+ array[3] + "\t" + array[4] + "\t" + array[5] + "\t"
//						+ array[6] + "\t" + interf + "\t" + array[8]);
//				out.flush();
				
			}

			proc.waitFor();
			System.out.println("Exit monitor");
			proc.destroy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
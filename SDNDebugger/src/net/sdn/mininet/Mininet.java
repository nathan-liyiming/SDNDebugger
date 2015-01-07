package net.sdn.mininet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Mininet {
	private static BufferedReader in;
	private static PrintWriter out;
	private static BufferedReader error;

	private static Map<String, PortPair> hosts = new HashMap<String, PortPair>();
	private static Map<String, List<PortPair>> switches = new HashMap<String, List<PortPair>>();

	public static void main(String[] args) {
		String line = null;
		try {
			System.out.println("Start the mininet.");
			Process proc = Runtime.getRuntime().exec(
					"mn/run_mininet.py " + args[0]);

			in = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));
			out = new PrintWriter(proc.getOutputStream());
			error = new BufferedReader(new InputStreamReader(
					proc.getErrorStream()));

			boolean flag = true;
			while (flag && (line = error.readLine()) != null) {
				net();
				while (flag && line.matches("^(s|h).*:.*")) {
					if (line.matches("^h.*")) {
						String parts[] = line.split(" ");
						String subParts[] = parts[1].split(":");
						hosts.put(parts[0], new PortPair(subParts[0],
								subParts[1]));
					} else {
						String parts[] = line.split(" +");
						List<PortPair> list = new LinkedList<PortPair>();
						for (int i = 2; i < parts.length; i++) {
							String subParts[] = parts[i].split(":");
							list.add(new PortPair(subParts[0], subParts[1]));
						}

						switches.put(parts[0], list);
					}

					line = error.readLine();
					if (line.matches("c0")) {
						flag = false;
					}
				}
			}

			/*
			 * for (String h : switches.keySet()) { System.out.print(h + " ");
			 * for (PortPair p : switches.get(h)) { System.out.print("  " +
			 * p.getLeft() + ":" + p.getRight()); } System.out.println(); }
			 */

			while ((line = error.readLine()) != null) {
				;
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

	public static Map<String, PortPair> getHosts() {
		return hosts;
	}

	public static Map<String, List<PortPair>> getSwitches() {
		return switches;
	}

	public static void net() {
		doOperation("net");
	}

	public static void pingall() {
		doOperation("pingall");
	}

	public static void stop() {
		doOperation("stop");
	}

	public static void doOperation(String s) {
		out.println("\"" + s + "\"");
		out.flush();
	}
}

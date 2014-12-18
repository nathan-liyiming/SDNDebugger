package net.sdn.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Monitor {
	private final static String TSHARKPARAMS[] = { "/usr/bin/sudo",
			"/usr/bin/tshark", "-T", "fields", "-e", "frame.interface_id",
			"-e", "eth.type", "-e", "eth.src", "-e", "eth.dst", "-e", "ip.src",
			"-e", "ip.dst" };

	public static void main(String args[]) {
		String line = null;
		try {
			System.out.println("Start the monitor.");
			Process proc = Runtime.getRuntime().exec(TSHARKPARAMS);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));

			while ((line = stdInput.readLine()) != null) {
				System.out.println(line);
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

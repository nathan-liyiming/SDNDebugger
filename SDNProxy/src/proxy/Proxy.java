package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.*;

public class Proxy {

	private int controllerPort; // port connect to the controller
	private int localPort; // port wait for connection
	private ServerSocket serverSocket;
	private Logger log;

	public Proxy(int ctrlPort, int locPort) {
		controllerPort = ctrlPort;
		localPort = locPort;
	}

	private class OFSwitchHandler {
		private Socket ofSwitchSocket;
		private Socket controllerSocket;
		private InputStream controllerInputStream;
		private OutputStream controllerOutputStream;
		private InputStream ofSwitchInputStream;
		private OutputStream ofSwitchOutputStream;
		private Thread upThread;
		private Thread downThread;
		private boolean isAlive = true;

		public OFSwitchHandler(Socket ofSwitch) throws UnknownHostException,
				IOException {
			ofSwitchSocket = ofSwitch;
			ofSwitchSocket.setSoTimeout(0); // block reading
			controllerSocket = new Socket("127.0.0.1", controllerPort);
			controllerSocket.setSoTimeout(0); // block reading
			controllerInputStream = controllerSocket.getInputStream();
			controllerOutputStream = controllerSocket.getOutputStream();
			ofSwitchInputStream = ofSwitchSocket.getInputStream();
			ofSwitchOutputStream = ofSwitchSocket.getOutputStream();
		}

		public void start() {
			upThread = new Thread(new Runnable() {

				@Override
				public void run() {
					byte[] buffer = new byte[1024];
					while (isAlive) {
						try {
							int len = ofSwitchInputStream.read(buffer);
							if (len >= 0) {
								controllerOutputStream.write(buffer, 0, len);
							}
						} catch (SocketException e) {
							isAlive = false;
							try {
								ofSwitchSocket.close();
								controllerSocket.close();
							} catch (IOException except) {
								except.printStackTrace();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							isAlive = false;
							e.printStackTrace();
						}
					}
				}

			});

			downThread = new Thread(new Runnable() {

				@Override
				public void run() {
					byte[] buffer = new byte[1024];
					while (isAlive) {
						try {
							int len = controllerInputStream.read(buffer);
							if (len >= 0) {
								ofSwitchOutputStream.write(buffer, 0, len);
							}

						} catch (SocketException e) {
							isAlive = false;
							try {
								controllerSocket.close();
								ofSwitchSocket.close();
							} catch (IOException except) {
								except.printStackTrace();
							}
						} catch (IOException e) {
							isAlive = false;
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			});
			upThread.start();
			downThread.start();
		}
	}

	public void start() {
		log = Logger.getLogger("ProxyLog");
		try {
			serverSocket = new ServerSocket(localPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.severe("Binding " + localPort + "Failed!");
			e.printStackTrace();
		}
		log.info("SDNDebugger Proxy is up! Waiting for OFSwitches...");
		Socket ofSwitch = null;
		int count = 0;
		while (true) {
			try {
				ofSwitch = serverSocket.accept();
				if (count++ == 0){
					continue;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			try {
				OFSwitchHandler of = new OFSwitchHandler(ofSwitch);
				of.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
	}

	public static void main(String[] args) {
		Proxy proxy = new Proxy(6633, 8000);
		proxy.start();
	}
}

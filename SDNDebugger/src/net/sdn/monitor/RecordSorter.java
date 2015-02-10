package net.sdn.monitor;

import java.io.PrintWriter;
import java.util.LinkedList;

public class RecordSorter extends Thread {

	private final static long interval = 200000000;

	private LinkedList<Pair> store = new LinkedList<Pair>();
	private PrintWriter out;

	private long split;

	public RecordSorter(PrintWriter out) {
		this.out = out;
	}

	public void insertRecord(long time, String recorder) {
		int i = 0;
		synchronized (this) {
			for (i = store.size() - 1; i >= 0; i--) {
				if (time > store.get(i).time) {
					break;
				}
			}
			store.add(i + 1, new Pair(time, recorder));
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			split = System.currentTimeMillis() * 1000000 - interval;
			synchronized (this) {
				while (store.size() != 0 && store.getFirst().time <= split) {
					String tmp = store.removeFirst().recorder;
					System.out.println(tmp);
					out.println(tmp);
					out.flush();
				}
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public class Pair {
		public long time;
		public String recorder;

		public Pair(long time, String recorder) {
			this.time = time;
			this.recorder = recorder;
		}
	}
}

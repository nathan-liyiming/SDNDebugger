package net.sdn.monitor;

import java.io.PrintWriter;
import java.util.LinkedList;

public class RecordSorter extends Thread {

	private final static double interval = 0.2;

	private LinkedList<Pair> store = new LinkedList<Pair>();
	private PrintWriter out;
	private double begin, end;
	private boolean flag;

	private double split;

	public RecordSorter(PrintWriter out) {
		this.out = out;
	}

	public void insetRecord(double time, String recorder) {
		if (flag) {
			begin = System.nanoTime();
			flag = false;
		}
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
			
			end = System.nanoTime();
			split = (end - begin) / Math.pow(10, 9) - interval;
//			split += (interval * 1000);
			synchronized (this) {
				while (store.size() != 0 && store.getFirst().time <= split) {
					out.println(store.removeFirst().recorder);
					out.flush();
				}
			}
			try {
				Thread.sleep((long)(interval * 1000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public class Pair {
		public double time;
		public String recorder;

		public Pair(double time, String recorder) {
			this.time = time;
			this.recorder = recorder;
		}
	}
}

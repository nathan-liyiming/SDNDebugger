package net.sdn.monitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;

import net.sdn.event.NetworkEvent;
import net.sdn.event.NetworkEventDirection;

import com.google.gson.Gson;

public class RecordSorter extends Thread {

	private final static long interval = 200000000;

	private LinkedList<Pair> store = new LinkedList<Pair>();
	private PrintWriter out;

	private long split;
	
	PrintWriter writer;

	public RecordSorter(PrintWriter out) {
		this.out = out;
		try {
			writer = new PrintWriter("/home/yli/SDNDebugger/SDNDebugger/a.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void insertRecord(long time, NetworkEvent recorder) {
		int i = 0;
		synchronized (this) {
			writer.println(System.currentTimeMillis() + "\t" + recorder.timeStamp / 1000000);
			writer.flush();
			for (i = store.size() - 1; i >= 0; i--) {
				if (time > store.get(i).time) {
					break;
				} else if (time == store.get(i).time) {
					if (recorder.direction == NetworkEventDirection.OUT) {
						// remove "in", add "out"
						store.remove(i);
					} else {
						// has added "out" before
						return;
					}
				}
			}
			store.add(i + 1, new Pair(time, recorder));
		}
	}

	@Override
	public void run() {
		while (true) {
			split = System.currentTimeMillis() * 1000000 - interval;
			synchronized (this) {
				while (store.size() != 0 && store.getFirst().time <= split) {
					NetworkEvent sEvt = store.removeFirst().recorder;
					
					// serialization
					Gson gson = new Gson();
					String json = gson.toJson(sEvt);
					
					System.out.println(json);
					out.println(json);
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
		public NetworkEvent recorder;

		public Pair(long time, NetworkEvent recorder) {
			this.time = time;
			this.recorder = recorder;
		}
	}
}

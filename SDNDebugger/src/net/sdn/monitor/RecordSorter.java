package net.sdn.monitor;

import java.util.LinkedList;

public class RecordSorter {
	LinkedList<String> store = new LinkedList<String>();
	
	public void insetRecord(String line) {
		store.add(line);
	}
	
	public String getRecord() {
		
	}
}

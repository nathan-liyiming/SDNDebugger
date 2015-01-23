package net.sdn.debugger;

public class PacketDeserializer {
	
	public Packet deserialize(String record){
		if(record.length() == 0)
			return null;
		
		Packet pkt = new Packet();
		String[] temp = record.split(" ");
		return pkt;
	}

}

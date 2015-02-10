package net.sdn.phytopo;

public class Link {
	public Node left;
	public String left_interf;
	public Node right;
	public String right_interf;
	
	public Link(String l_i, Node l, String r_i, Node r){
		left = l;
		left_interf = l_i;
		right = r;
		right_interf = r_i;
	}

}

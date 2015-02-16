package net.sdn.phytopo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sdn.policy.Policy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

public class PhyTopo {
	private HashMap<String, Host> hosts = new HashMap<String, Host>();
	private HashMap<String, Switch> switches = new HashMap<String, Switch>();
	private ArrayList<Link> links = new ArrayList<Link>();
	private HashMap<String, Controller> controllers = new HashMap<String, Controller>();

	public PhyTopo(String topoFile) {
		try {
			parseTopoFile(topoFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parseTopoFile(String topoFile) throws IOException {
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			Document dom = db.parse(topoFile);

			// get the root element
			Element docEle = dom.getDocumentElement();

			// get a nodelist of elements
			addHosts(docEle);
			addSwitches(docEle);
			addLinks(docEle);
			addControllers(docEle);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public net.sdn.phytopo.Node getNode(String node) {
		if (hosts.containsKey(node))
			return hosts.get(node);
		else if (switches.containsKey(node))
			return switches.get(node);
		return null;
	}

	public Switch getSwitch(String sw) {
		return switches.get(sw);
	}

	public Host getHost(String h) {
		return hosts.get(h);
	}

	public void addHosts(Element docEle) {
		NodeList nList = docEle.getElementsByTagName("host");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = (Node) nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				String id = eElement.getElementsByTagName("id").item(0)
						.getTextContent();
				String nw_addr = eElement.getElementsByTagName("nw_addr")
						.item(0).getTextContent();
				String dl_addr = eElement.getElementsByTagName("dl_addr")
						.item(0).getTextContent();

				Host host = new Host(id, dl_addr, nw_addr);
				hosts.put(id, host);

			}
		}
	}

	public void addSwitches(Element docEle) {
		NodeList nList = docEle.getElementsByTagName("switch");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = (Node) nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				String id = eElement.getElementsByTagName("id").item(0)
						.getTextContent();
				String dpid = eElement.getElementsByTagName("dpid").item(0)
						.getTextContent();

				Switch sw = new Switch(id, dpid);
				switches.put(id, sw);
			}
		}
	}

	public void addLinks(Element docEle) {
		NodeList nList = docEle.getElementsByTagName("link");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = (Node) nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				String left = eElement.getElementsByTagName("left").item(0)
						.getTextContent();
				String right = eElement.getElementsByTagName("right").item(0)
						.getTextContent();
				String left_interf = eElement
						.getElementsByTagName("left_interf").item(0)
						.getTextContent();
				String right_interf = eElement
						.getElementsByTagName("right_interf").item(0)
						.getTextContent();

				Link link = new Link(left_interf.split("-")[1], getNode(left),
						right_interf.split("-")[1], getNode(right));

				// Add port to switches
				if (getNode(left).getType().equalsIgnoreCase("s")) {
					getSwitch(left).addPort(left_interf.split("-")[1]);
				} else {
					getHost(left).setPort(right_interf.split("-")[1]);
					getHost(left).setSwitch(getSwitch(right));
				}

				if (getNode(right).getType().equalsIgnoreCase("s")) {
					getSwitch(right).addPort(right_interf.split("-")[1]);
				} else {
					getHost(right).setPort(left_interf.split("-")[1]);
					getHost(right).setSwitch(getSwitch(left));
				}

				links.add(link);
			}
		}
	}

	public void addControllers(Element docEle) {
		NodeList nList = docEle.getElementsByTagName("controller");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = (Node) nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				String id = eElement.getElementsByTagName("id").item(0)
						.getTextContent();
				String nw_addr = eElement.getElementsByTagName("nw_addr")
						.item(0).getTextContent();
				String port = eElement.getElementsByTagName("port").item(0)
						.getTextContent();

				Controller controller = new Controller(id, nw_addr, port);
				controllers.put(id, controller);
			}
		}
	}

	public HashMap<String, Host> getHosts() {
		return hosts;
	}

	public HashMap<String, Switch> getSwitches() {
		return switches;
	}

	public ArrayList<Link> getLinks() {
		return links;
	}

	public HashMap<String, Controller> getControllers() {
		return controllers;
	}

	public void addPolicyToSwitch(String s, Policy p) {
		getSwitch(s).addPolicy(p);
	}
	 
}

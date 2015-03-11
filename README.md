Simon: Scriptable Interactive Monitoring for SDNs
=================================================

What is Simon?
--------------
We introduce SIMON, a Scriptable Interactive Monitoring system for SDN. With SIMON, operators can probe their network behavior by executing scripts for debugging, monitoring, and more. SIMON is independent of the controller platform used, and does not require annotations or intimate knowledge of the controller software being run. Operators may compose debugging scripts both offline and interactively at SIMON’s debugging prompt. In the process, they can take advantage of the rich set of reactive functions SIMON provides as well as the full power of Scala.

Features
--------
1.	Interactive: SIMON exposes networking events as streams which users can explore.

2.	Scriptable: users can automate repetitive debugging tasks.

3.	Visible: SIMON exposes data-plane events, control-plane events and northbound API messages.

4.	Black-box testing: it does not assume the user is knowledgeable about intricacies of the controller being debugged.

5.	Powerful: support 1.0 - 1.3 OpenFlow messages and all the controllers. We have tested FlowLog (1.0), Ryu (1.1 and 1.3), FloodLight (1.3) and RouteFlow(Quagga) with RFProxy.

SIMON API functions
-------------------

Installation
------------
To use our SDN debugger (under Ubuntu), you need:

*	Mininet: 2.10+ at: http://mininet.org/download/

*	Java 1.7+ at: https://java.com/en/download/

*	Scala 2.11.X or higher: http://www.scala-lang.org/download/

*	Controller: [__FlowLog__](https://github.com/tnelson/FlowLog) for flowlog example, [__Ryu__](https://github.com/osrg/ryu) for reactive fireware and state fireware, [__Floodlight__](https://github.com/Sherkyoung/floodlight-plus) and [__RouteFlow__](https://sites.google.com/site/routeflow/downloads) for shorest path. Note: Installing Ryu can have problems, e.g. https://github.com/okfn/piati/issues/65#issuecomment-41514608. For RouteFlow, suggest to download pre-configured VM in different VMs from our Debugger.

How to run?
-----------
1.	Compile the Monitor and Debugger:

	```
	$ cd SDNDebugger
	$ ./compile.sh
	```

2.	Run controller, one terminal(T1) runs controller with app.


3.	Run our debugger in the new terminal(T2) and wait for connection:

	```
	$ ./simon.sh
	```

4. 	Another terminal(T3) connects listening debugger, runs Mininet and Monitor

	```
	$ sudo ./run.sh
	```

5.	In T3, get packet and do some oracle checking now.

Examples
--------
	
Limitation
----------
1.	Our monitor only supports ETH, ARP, IP, ICMP, TCP, UDP, OF_PACKET(PACKET_IN, PACKET_OUT, FLOW_MOD, ECHO_REQUEST, ECHO_REPLY)

2.	We have keep two time clock, one is depending the timestamp from jNetPcap and time in scala.	

Problems
--------
*	Q: 	Why does controller with app report some version problem?

	A: 	Make sure you use the corresponding version and do ```sudo mn -c``` to clear the running mininet.

*	Q:	Why does controller or simon report some connecting issue?

	A:	You should take care about running order (T1 -> T2 -> T3 see above).


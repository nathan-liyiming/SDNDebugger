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

5.	Compatible: support 1.0 - 1.3 OpenFlow messages and all the controllers. We have tested FlowLog (1.0), Ryu (1.1 and 1.3), FloodLight (1.3) and RouteFlow(Quagga, 1.0) with RFProxy.

API functions
-------------
1.	Based functions from reactive Scala

	*	_filter_: Applies a function to every event in the stream, keeping only events on which the function returns true.

	*	_map_: Applies a function to every event in the stream, keeping only events on which the function returns true.

	*	_subscribe_: Applies a function to every event in the stream, keeping only events on which the function returns true.

	*	_cache_: Cache can start to cache when the first subscribe happens, then we can replay.

	*	More: [details of observable](http://reactivex.io/documentation/observable.html) and [API of observable](http://reactivex.io/rxscala/scaladoc/index.html#rx.lang.scala.Observable)

2.	Based functions from Simon

	*	_expect_: Expect to see an event matching pred within duration d.

	*	_expectNot_: Not expect to see an event matching pred within duration d.

	*	_cpRelatedTo_: Produce related PacketIn, PacketOut, and FlowMod messages corresponding to an event.

	*	_printwarning_: Red means violation, green means success and blue means unknown event.

3.	Other helper functions

	*	_openShowEvents_: Open another xterm and print the the user observable stream.

	*	_isArpNetworkEvents_, _isDHCPNetworkEvents_, _isICMPNetworkEvents_, _isRESTNetworkEvents_, _isOFNetworkEvents_: judge whether the network event is what the user needs.


Installation
------------
To use our SDN debugger (under Ubuntu), you need:

*	Mininet: 2.10+ at: http://mininet.org/download/

*	Java 1.7+ at: https://java.com/en/download/

*	Scala 2.11.X or higher: http://www.scala-lang.org/download/

*	Controller: [__FlowLog__](https://github.com/tnelson/FlowLog) for flowlog example, [__Ryu__](https://github.com/osrg/ryu) for reactive fireware and state fireware, [__Floodlight__](https://github.com/Sherkyoung/floodlight-plus) and [__RouteFlow__](https://sites.google.com/site/routeflow/downloads) for shorest path. Note: Installing Ryu can have problems, e.g. https://github.com/okfn/piati/issues/65#issuecomment-41514608. For RouteFlow, suggest to download pre-configured VM in different VMs from our Debugger.

How to run and debug?
--------------------
1.	Define own topo(_topo.xml_, following format in examples) and application

2.	Compile the Monitor and Debugger:

	```
	$ cd SDNDebugger
	$ ./compile.sh
	```

3.	Run controller, one term(T1) runs controller with app.


4.	Run our debugger in the new term(T2) and wait for connection:

	```
	$ ./simon.sh
	```

5. 	Another term(T3) connects listening debugger, runs Mininet and Monitor

	```
	$ sudo ./run.sh
	```

6.	In T3, get packet and do some oracle checking now.

Examples
--------
__1.	State Firewall__

 	One bug is both apps in SF and SF10, where 10 means 1.0 OpenFlow. We just have one switch and two hosts. ```s1-eth1``` allows to pass through, but ```s1-eth2``` denies any passing. Once, the first packet is sent to ```s1-eth1```, then go to controller, which sends out two Flow_Mod rules to allow both ports of ```s1```. However, app ignores to send out the first packet leading to the bug. In order to capture it, we do as follows:

	__Step 1__: run controller in T1

	```
	$ ryu-manager state_firewall.py
	```

	__Step 2__: run debugger in T2

	```
	$ ./simon.sh
	```

	__Step 3__: until T2 prints ``Rx server started at port: 8200'', run Mininet and Monitor in T3

	```
	$ sudo ./run.sh
	```

	__Step 4__: run state firewall ideal model

	```
	scala> new SFIdeal("s1", Set("eth1"), Set("eth2"))
	```

	__Step 5__: try to capture OpenFlow message related to ICMP event

	```
	scala> ShowEvents.openShowEvents(Simon.nwEvents().
		   filter(SimonHelper.isICMPNetworkEvents).flatMap(Simon.cpRelatedTo))
	```

	__Step 6__: try to h1 ping h2 once in T3-mn-xterm

	```
	mininet> h1 ping h2 -c 1
	```

	__Step 7__: display Packet_In, Flow_Mod, miss Packet_Out. Now, check state_firewall.py and find:

	```
	datapath.send_msg(out)
	```

	__Note__: we can also run mininet in Scala after running mn.

	```
	scala> ./mininet/util/m h1 ifconfig
	scala> ./mininet/util/m h1 ping -c 1 10.0.0.2
	```

__2.	Reactive Firewall__
	
	We also created an ideal model in SIMON for the reactive firewall module released with the Ryu controller plat-form. This module accepts packet-filtering rules via HTTP messages, which it then enforces with corresponding OpenFlow rules on firewall switches. See: http://osrg.github.io/ryu-book/en/html/rest_firewall.html

	<p align="center">
  		<img src="http://osrg.github.io/ryu-book/en/html/_images/fig13.png" alt="Reactive Firewall" height="300" width="400" />
	</p>

	__Step 1__: run controller in T1

	```
	$ ryu-manager rest_firewall.py
	```

	__Step 2__: run debugger in T2

	```
	$ ./simon.sh
	```

	__Step 3__: until T2 prints ``Rx server started at port: 8200'', run Mininet and Monitor in T3. In addition, we add REST Monitor to capture HTTP message for changing rules.

	```
	$ sudo ./run.sh
	```

	__Step 4__: run reactive firewall ideal model

	```
	scala> new RFIdeal("s1")
	```

	__Step 5__: try to h1 ping h2 once in T3-mn-xterm, it will fail and filter OpenFlow messages, you don't find any Flow_Mod

	```
	mininet> h1 ping h2 -c 1
	```

	__Step 6__: try to send REST command to add rules, allow passing from h1 to h2 and from h2 to h1
	```
	$ ./enable.sh

	$ ./add.sh
	```

	__Step 7__: try to h1 ping h2 once in T3-mn-xterm, it will be successful, you will see Flow_Mod messages.

	```
	mininet> h1 ping h2 -c 1
	```

__3.	Shortest-Path Routing__
	
	We have two kinds of controller(application) for testing: _FloodLight_ and _RouteFlow_ with RFProxy. We modify the [final project-SDN](http://cs.brown.edu/courses/cs168/f14/content/projects/sdn.pdf) from Computer Networks (CS168 Fall 2014) in the Department of Computer Science at Brown to compute the shortest path by Bellman–Ford algorithm. For RouteFlow VM, be careful to set the same gateway of Debugger VM. The topo is coming from Tutorial-2 of RouteFlow: https://github.com/CPqD/RouteFlow/wiki/Tutorial-2:-rftest2
	
	<p align="center">
  		<img src="https://raw.githubusercontent.com/wiki/CPqD/RouteFlow/images/rftest2_scenario.png" alt="Reactive Firewall" height="300" width="400" />
	</p>

	__Step 1__: run controller in T1

	```
	$ java -jar FloodlightWithApps.jar -cf shortestPathSwitching.prop
	```
	Or
	```
	$ sudo ./rftest2 (in RouteFlow VM)
	```	

	__Step 2__: run debugger in T2

	```
	$ ./simon.sh
	```

	__Step 3__: until T2 prints ``Rx server started at port: 8200'', run Mininet and Monitor in T3

	```
	$ sudo ./run_floodlight.sh
	```
	```
	$ sudo ./run_routeflow.sh
	```

	__Step 4__: run shortest path ideal model

	```
	scala> new SPIdeal("../examples/SP/topo.xml")
	```

	__Step 5__: try to h1 ping h2 once or pingall in T3-mn-xterm

	```
	mininet> h1 ping h2 -c 1
	mininet> pingall
	```
	
Limitation
----------
1.	Our monitor only supports ETH, ARP, IP, ICMP, TCP, UDP, OF_PACKET(Packet_In, Packet_Out, Flow_Mod, Echo_Request, Echo_Reply)

2.	We have keep two time clock, one is depending the timestamp from jNetPcap and time in scala.	

Problems
--------
*	Q: 	Why does controller with app report some version problem?

	A: 	Make sure you use the corresponding version and do ```sudo mn -c``` to clear the running mininet.

*	Q:	Why does controller or simon report some connecting issue?

	A:	You should take care about running order (T1 -> T2 -> T3 see above).


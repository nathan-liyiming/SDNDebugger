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

Examples
--------
	

--------
TODO: update this readme to include installation instructions.
Note that the examples require Ryu. Installing Ryu can have problems,
e.g. https://github.com/okfn/piati/issues/65#issuecomment-41514608

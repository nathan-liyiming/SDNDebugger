#!/usr/bin/env python
"""
Custom topologies for cs168, Fall 2014, Final Project
"""

import sys
from mininet.cli import CLI
from mininet.net import Mininet
from mininet.node import OVSSwitch, RemoteController
from mininet.topo import Topo,SingleSwitchTopo,LinearTopo
from mininet.topolib import TreeTopo
from mininet.log import setLogLevel, info
from mininet.util import customConstructor,quietRun

class rftest2(Topo):
    "RouteFlow Demo Setup"

    def __init__( self, enable_all = True ):
        "Create custom topo."

        Topo.__init__( self )

        h1 = self.addHost("h1",
                          ip="172.31.1.100/24",
                          defaultRoute="gw 172.31.1.1")

        h2 = self.addHost("h2",
                          ip="172.31.2.100/24",
                          defaultRoute="gw 172.31.2.1")

        h3 = self.addHost("h3",
                          ip="172.31.3.100/24",
                          defaultRoute="gw 172.31.3.1")

        h4 = self.addHost("h4",
                          ip="172.31.4.100/24",
                          defaultRoute="gw 172.31.4.1")

        sA = self.addSwitch("s5")
        sB = self.addSwitch("s6")
        sC = self.addSwitch("s7")
        sD = self.addSwitch("s8")

        self.addLink(h1, sA)
        self.addLink(h2, sB)
        self.addLink(h3, sC)
        self.addLink(h4, sD)
        self.addLink(sA, sB)
        self.addLink(sB, sD)
        self.addLink(sD, sC)
        self.addLink(sC, sA)
        self.addLink(sA, sD)

def starthttp(host):
    "Start simple Python web server on hosts"
    info( '*** Starting SimpleHTTPServer on host', host, '\n' )
    command = 'mkdir /tmp/%s; cp ./webserver.py /tmp/%s; pushd /tmp/%s/; echo WEB PAGE SERVED BY: %s > index.html; nohup python2.7 ./webserver.py &' % (host.name, host.name, host.name, host.name)
#    command = 'nohup python2.7 ./webserver.py'
    host.cmd(command)

def stophttp():
    "Stop simple Python web servers"
    info( '*** Shutting down stale SimpleHTTPServers', 
          quietRun( "pkill -9 -f SimpleHTTPServer" ), '\n' )    
    info( '*** Shutting down stale webservers', 
          quietRun( "pkill -9 -f webserver.py" ), '\n' )    
   
if __name__ == '__main__':
    setLogLevel( 'info' )

    # Create network
    topo = rftest2()
    net = Mininet(topo=topo, autoSetMacs=True, controller=RemoteController,
            switch=customConstructor({'ovsk' : OVSSwitch}, "ovsk,protocols=OpenFlow13"))

    # Run network
    net.start()
    for h in net.hosts:
	h.cmd('route add default gw 172.31.%s.1 dev %s-eth0' % (h.name[-1], h.name))
        info('*** ARPing from host %s\n' % (h.name))
        h.cmd('arping -c 2 -A -I '+h.name+'-eth0 '+h.IP())
        starthttp(h)
    CLI( net )
    stophttp()
    net.stop()


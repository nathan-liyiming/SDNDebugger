xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger ../examples/Firewall/topo.xml &
sleep 1
xterm -T "mininet" -e mn --topo=single,3 --controller=remote,ip=127.0.0.1,port=6633 --switch=ovsk,protocols=OpenFlow13 &
sleep 5
xterm -T "monitor" -e java -Djava.library.path="lib/jnetpcap-1.4.r1425" -cp "bin:lib/commons-cli-1.2.jar:lib/junit-4.8.1.jar:lib/netty-all-4.0.24.Final.jar:lib/rx-netty-0.3.18.jar:lib/rx.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/jnetpcap-1.4.r1425/jnetpcap.jar:lib/gson-2.3.1.jar" net.sdn.monitor.Monitor ../examples/Firewall/topo.xml &


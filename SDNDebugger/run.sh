xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger ../examples/Firewall/topo.xml &
sleep 1
xterm -T "mininet" -e mn/run_mininet.py single,3 &
sleep 5
xterm -T "monitor" -e java -Djava.library.path="lib/jnetpcap-1.4.r1425" -cp "bin:lib/*.jar:lib/jnetpcap-1.4.r1425/jnetpcap.jar:lib/gson-2.3.1.jar" net.sdn.monitor.Monitor ../examples/Firewall/topo.xml &


xterm -T "Debugger" -e java -cp "../../SDNDebugger/bin:../../SDNDebugger/lib/*" net.sdn.debugger.SFVerifier ./topo.xml &
sleep 1
xterm -T "Mininet" -e mn --topo=single,2 --mac --controller=remote,ip=127.0.0.1,port=6633 --switch=ovsk,protocols=OpenFlow13 &
sleep 5
xterm -T "Monitor" -e java -Djava.library.path="../../SDNDebugger/lib/jnetpcap-1.4.r1425" -cp "../../SDNDebugger/bin:../../SDNDebugger/lib/commons-cli-1.2.jar:../../SDNDebugger/lib/junit-4.8.1.jar:../../SDNDebugger/lib/netty-all-4.0.24.Final.jar:../../SDNDebugger/lib/rx-netty-0.3.18.jar:../../SDNDebugger/lib/rx.jar:../../SDNDebugger/lib/slf4j-api-1.7.10.jar:../../SDNDebugger/lib/slf4j-simple-1.7.10.jar:../../SDNDebugger/lib/jnetpcap-1.4.r1425/jnetpcap.jar:../../SDNDebugger/lib/gson-2.3.1.jar:../../SDNDebugger/lib/openflowj.jar" net.sdn.monitor.Monitor ./topo.xml &


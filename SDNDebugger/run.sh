xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger &
sleep 1
xterm -T "proxy" -e java -cp "bin:lib/*" net.sdn.proxy.Proxy &
sleep 1
xterm -T "mininet" -e mn --topo=linear,2 --controller=remote,ip=127.0.0.1,port=8000 --switch=ovsk,protocols=OpenFlow13 &
sleep 10
xterm -T "monitor" -e java -cp "bin:lib/*" net.sdn.monitor.Monitor &


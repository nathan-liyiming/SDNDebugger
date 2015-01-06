xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger &
sleep 1
xterm -T "proxy" -e java -cp "bin:lib/*" net.sdn.proxy.Proxy &
sleep 1
xterm -T "mininet" -e mn/run_mininet.py linear,2 &
sleep 10
xterm -T "monitor" -e java -cp "bin:lib/*" net.sdn.monitor.Monitor &


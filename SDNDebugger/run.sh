xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger &
sleep 1
xterm -T "proxy" -e java -cp "bin:lib/*" net.sdn.proxy.Proxy &
sleep 1
<<<<<<< HEAD
xterm -T "mininet" -e java -cp "bin:lib/*" net.sdn.mininet.Mininet linear,2 &
sleep 15
=======
xterm -T "mininet" -e mn/run_mininet.py single,2 &
sleep 10
>>>>>>> CLI
xterm -T "monitor" -e java -cp "bin:lib/*" net.sdn.monitor.Monitor &


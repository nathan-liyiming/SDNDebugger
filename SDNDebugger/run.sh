xterm -T "debugger" -e java -cp "bin:lib/*" net.sdn.debugger.Debugger &
sleep 2
xterm -T "proxy" -e java -cp "bin:lib/*" net.sdn.proxy.Proxy &


#mkdir bin
#javac -d bin -sourcepath src -cp "bin:lib/commons-cli-1.2.jar:lib/junit-4.8.1.jar:lib/netty-all-4.0.24.Final.jar:lib/rx-netty-0.3.18.jar:lib/rx.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/jnetpcap-1.4.r1425/jnetpcap.jar:lib/gson-2.3.1.jar:lib/openflowj-0.9.0-SNAPSHOT.jar:lib/netty-3.2.6.Final.jar:lib/guava-13.0.1.jar" src/net/sdn/debugger/*.java src/net/sdn/monitor/*.java
#scalac -classpath "bin" Foo.scala
scalac -classpath "bin:lib/commons-cli-1.2.jar:lib/junit-4.8.1.jar:lib/netty-all-4.0.24.Final.jar:lib/rx-netty-0.3.18.jar:lib/rx.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/jnetpcap-1.4.r1425/jnetpcap.jar:lib/gson-2.3.1.jar:lib/openflowj-0.9.0-SNAPSHOT.jar:lib/netty-3.2.6.Final.jar:lib/guava-13.0.1.jar" simon.scala

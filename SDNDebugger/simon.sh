#!/bin/sh
exec scala -cp "bin:lib/rxscala_2.11-0.23.1.jar:lib/commons-cli-1.2.jar:lib/junit-4.8.1.jar:lib/netty-all-4.0.24.Final.jar:lib/rx-netty-0.3.18.jar:lib/rx.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/jnetpcap-1.4.r1425/jnetpcap.jar:lib/gson-2.3.1.jar:lib/openflowj-0.9.0-SNAPSHOT.jar:lib/netty-3.2.6.Final.jar:lib/guava-13.0.1.jar" -i Simon.scala
!#
#Run SIMON on module load in REPL 
#   (don't make the user type Simon.run() every time.)
Simon.run();
println("SIMON loaded!");

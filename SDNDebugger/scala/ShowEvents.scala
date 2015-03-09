/*
	Inspired by Commonwealth Bank of Australia's named pipe implementation:
	https://github.com/CommBank/piped/blob/master/src/main/scala/com/cba/omnia/piped/utils/NamedPipe.scala

	Can't just do stdin/out with xterm. Instead, need to use a named pipe.
*/

import scala.sys.process._
import java.util.UUID
import java.io._
import rx.lang.scala.Observable;

// TODO: should use /tmp directory
object ShowEvents {
	def openShowEvents(o: Observable[Event]) {
		val pipename = s"simon_temp${UUID.randomUUID}.pipe"
		val pipefile = new File(".", pipename);

    	// Receiver must call mkfifo
    	// tail -f didn't work here, but cat did. (If normal file, cat will terminate on EOF.)
    	Process(List("xterm","-T", "Events", "-e", s"""trap "rm -f ./${pipename}" EXIT; mkfifo ./${pipename}; cat ./${pipename}""")).run

    	// Without a delay here, this app starts writing to the file before
    	// the xterm makes it a pipe. tail -f works, cat does not, and file grows in size.
    	while(!pipefile.exists()) {
    		Thread sleep 50
    	}

  		// This must be done _after_ the other process runs. Will block until other end of the pipe is open.
   		val writer = new PrintWriter(s"${pipename}")

   		// Exit cleanly
   		sys.addShutdownHook({ writer.close();
   							  pipefile.delete(); })

   		o.subscribe({e => writer.println(s"${e.toString()}");
   						  if(writer.checkError()) 
   						  // println("error encountered!");
   						  // TODO: when process exits, we need to close write and unsubscribe
   						  })

	}

}

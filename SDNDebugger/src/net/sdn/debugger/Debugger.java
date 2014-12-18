/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sdn.debugger;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.io.OFMessageAsyncStream;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.factory.BasicFactory;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.server.RxServer;
import rx.Notification;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * @author Tomasz Bak
 * @author Da Yu, Yiming Li
 */
public final class Debugger {

    static final int DEFAULT_PORT = 8100;

    private final int port;
    
    private BasicFactory factory;
    
    private List<OFMessage> list;
    
    private ByteBuffer proxyIncomingBuffer;

    public Debugger(int port) {
        this.port = port;
        this.factory = BasicFactory.getInstance();
        proxyIncomingBuffer = ByteBuffer
				.allocateDirect(OFMessageAsyncStream.defaultBufferSize);
    }

    public RxServer<ByteBuf, ByteBuf> createServer() {
        RxServer<ByteBuf, ByteBuf> server = RxNetty.createTcpServer(port, new ConnectionHandler<ByteBuf, ByteBuf>() {
            @Override
            public Observable<Void> handle(
                    final ObservableConnection<ByteBuf, ByteBuf> connection) {
                System.out.println("Proxy connection established.");
                return connection.getInput().flatMap(new Func1<ByteBuf, Observable<Notification<Void>>>() {
                    @Override
                    public Observable<Notification<Void>> call(ByteBuf msg) {
                        ByteBuffer inBuf = msg.nioBuffer();
                        proxyIncomingBuffer.put(inBuf);
                        proxyIncomingBuffer.flip();
                        list = factory.parseMessages(proxyIncomingBuffer, 0);
                        if (proxyIncomingBuffer.hasRemaining())
                        	proxyIncomingBuffer.compact();
                        else
                        	proxyIncomingBuffer.clear();
                        printOFMessages(list);
                        return Observable.empty();
                    }
                })
                .takeWhile(new Func1<Notification<Void>, Boolean>() {
                	@Override
                	public Boolean call(Notification<Void> notification) {
                		return !notification
                				.isOnError();
                		}
                	})
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        System.out.println(" --> Closing Debugger and stream");
                    }
                }).map(new Func1<Notification<Void>, Void>() {
                    @Override
                    public Void call(Notification<Void> notification) {
                        return null;
                    }
                });
            }
        });

        System.out.println("Debugger started...");
        return server;
    }

    public static void main(final String[] args) {
        new Debugger(DEFAULT_PORT).createServer().startAndWait();
    }
    
    public void printOFMessages(List<OFMessage> msgs){
    	for (OFMessage m : msgs) {
    		switch (m.getType()) {
			case PACKET_IN:
				System.err.println("GOT PACKET_IN");
				System.err.println("--> Data:"
						+ ((OFPacketIn) m).toString());
				break;
			case FEATURES_REPLY:
				System.err.println("GOT FEATURE_REPLY");
				System.err.println("--> Data:"
						+ ((OFFeaturesReply) m).toString());
				break;
			case STATS_REPLY:
				System.err.println("GOT STATS_REPLY");
				System.err.println("--> Data:"
						+ ((OFStatisticsReply) m).toString());
				break;
			case HELLO:
				System.err.println("GOT HELLO");
				break;
			case ERROR:
				System.err.println("GOT ERROR");
				System.err.println("--> Data:"
						+ ((OFError) m).toString());
				break;
			default:
				System.err.println("Unhandled OF message: "
						+ m.getType());
			}
    	}
    }
}
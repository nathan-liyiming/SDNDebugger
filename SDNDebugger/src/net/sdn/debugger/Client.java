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

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.sdn.debugger.Debugger.*;

/**
 * @author Tomasz Bak
 */
public class Client {

    private final int port;

    public Client(int port) {
        this.port = port;
    }

    public List<String> sendEchos() {
        RxClient<String, String> rxClient = RxNetty.<String, String>newTcpClientBuilder("localhost", port)
                .pipelineConfigurator(PipelineConfigurators.textOnlyConfigurator())
                .build();

        Iterable<Object> echos = rxClient.connect().flatMap(new Func1<ObservableConnection<String, String>, Observable<?>>() {
            @Override
            public Observable<?> call(final ObservableConnection<String, String> connection) {

                // output 10 values at intervals and receive the echo back
                Observable<String> intervalOutput =
                        Observable.interval(5, TimeUnit.SECONDS)
                                .flatMap(new Func1<Long, Observable<String>>() {
                                    @Override
                                    public Observable<String> call(Long aLong) {
                                        return connection.writeAndFlush(String.valueOf(aLong + 1))
                                                .map(new Func1<Void, String>() {
                                                    @Override
                                                    public String call(Void aVoid) {
                                                        return "";
                                                    }
                                                });
                                    }
                                });

                // wait for the helloMessage then start the output and receive echo input
                return intervalOutput;
            }
        }).finallyDo(new Action0() {
            @Override
            public void call() {
                System.out.println(" --> Closing Debugger and stream");
            }
        }).take(1).doOnCompleted(new Action0() {
            @Override
            public void call() {
                System.out.println("COMPLETED!");
            }
        }).toBlocking().toIterable();

        List<String> result = new ArrayList<String>();
        for (Object e : echos) {
            System.out.println(e);
            result.add(e.toString());
        }
        System.out.println(result.size());
        return result;
    }

    public static void main(String[] args) {
        new Client(DEFAULT_PORT).sendEchos();
    }

}

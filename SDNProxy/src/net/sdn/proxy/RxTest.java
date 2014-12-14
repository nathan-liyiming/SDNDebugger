package net.sdn.proxy;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

// help links
// https://github.com/ReactiveX/RxJava/wiki/Scheduler
// http://www.grahamlea.com/2014/07/rxjava-threading-examples/
// http://blog.danlew.net/2014/09/22/grokking-rxjava-part-2/

public class RxTest {
	public static void main(String args[]) throws InterruptedException {
		List<String> list = new ArrayList<String>();
		for (int i = 1; i <= 100; i++) {
			list.add(i + "");
		}

		Subscriber<String> mySubscriber = new Subscriber<String>() {
			@Override
			public void onNext(String s) {
				System.out.println(s);
			}

			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
			}
		};

		createObservable(list).observeOn(Schedulers.computation()).map(new Func1<String, String>() {
			@Override
			public String call(String s) {
				return s + " _^ ^_";
			}
		}).subscribe(mySubscriber);
		
		System.out.println("Main thread done!");
		// make sure it doesn't kill observer
		Thread.sleep(5000);
	}

	public static Observable<String> createObservable(List<String> list) {
		return Observable.from(list);
	}
}

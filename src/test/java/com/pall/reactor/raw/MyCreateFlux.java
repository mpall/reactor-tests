package com.pall.reactor.raw;

import org.reactivestreams.Subscriber;

public class MyCreateFlux<T> extends MyFlux<T> {

    private MyPublisher<T> publisher;

    public MyCreateFlux(MyPublisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(s);
    }

}

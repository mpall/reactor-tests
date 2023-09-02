package com.pall.reactor.raw;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class MyCreateFlux<T> extends MyFlux<T> {

    private Publisher<T> publisher;

    public MyCreateFlux(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(s);
    }

}

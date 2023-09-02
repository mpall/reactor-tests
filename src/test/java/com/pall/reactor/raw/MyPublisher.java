package com.pall.reactor.raw;

import java.util.Arrays;
import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

//Publisher create to understand how publishers interact with subscribers. This is not an example of good coding practices. 
public class MyPublisher<T> implements Publisher<T>, Subscription{

    private Subscriber<? super T> subscriber;
    private long requested = 0;
    private boolean cancelled = false;

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    private void nextInternal(T element) {
        Objects.requireNonNull(subscriber, "Attempted to publish element prior to publisher being subscribed to");
        if (cancelled) {throw new RuntimeException("Publisher cancelled by subscriber");}
        if (requested == 0) {throw new RuntimeException("More elements published than requested");}
        subscriber.onNext(element);
        requested--;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public void request(long requested) {
        this.requested = this.requested + requested;
        
    }

    @SuppressWarnings("unchecked")
    public void next(T... elements ) {
        Arrays.stream(elements).forEach(this::nextInternal);       
    }

    public void complete() {
        subscriber.onComplete();
    }

    public void error(Throwable throwable) {
        subscriber.onError(throwable);
    }
    
    

}

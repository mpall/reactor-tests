package com.pall.reactor.raw;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

//Subscriber create to understand how publishers interact with subscribers. This is not an example of good coding practices.
public class MySubscriber<T> implements Subscriber<T> {
    private List<T> onNextElements = new LinkedList<>();
    private Subscription subscription = null;
    private boolean onComplete = false;
    private Throwable throwable = null;
    
    @Override
    public void onComplete() {
        onComplete = true;
    }

    @Override
    public void onError(Throwable throwable) {
        this.throwable = throwable;        
    }

    @Override
    public void onNext(T t) {
        onNextElements.add(t);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        
    }

    public void request(int i) {
    	if(i < 0) {throw new RuntimeException("Negative value passed to request");}
        Objects.requireNonNull(subscription, "Subscription has not been passed to Subscriber")
            .request(i);
    }
    
    public void cancel() {
        Objects.requireNonNull(subscription, "Subscription has not been passed to Subscriber")
            .cancel();
    }

    public List<T> getOnNextElements() {
        return onNextElements;
    }
    
    public boolean isOnComplete() {
        return onComplete;
    }
    
    public Throwable getThrowable() {
        return throwable;
    }


}

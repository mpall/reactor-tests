package com.pall.reactor.raw;

import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.util.annotation.NonNull;

//Operation that does nothing other than to pass signals through.
//Created to discover that basics of an operation.
public class MyNOOPOperation<T> implements Publisher<T> {

    private MyPublisher<T> sourcePublisher;
    
    public MyNOOPOperation(MyPublisher<T> sourcePublisher) {
        super();
        this.sourcePublisher = sourcePublisher;
    }
    @Override
    public void subscribe(Subscriber<? super T> targetSubscriptionSubscriber) {
        Objects.requireNonNull(sourcePublisher, "Source Publisher has not been set in Operation")
            .subscribe(new Subscriber<T>() {
    
                @Override
                public void onComplete() {
                    targetSubscriptionSubscriber.onComplete();
                }
    
                @Override
                public void onError(Throwable throwable) {
                    targetSubscriptionSubscriber.onError(throwable);
                    
                }
    
                @Override
                public void onNext(T element) {
                    targetSubscriptionSubscriber.onNext(element);
                }
    
                @Override
                public void onSubscribe(Subscription sourcePublisherSubscription) {
                    targetSubscriptionSubscriber.onSubscribe(new Subscription() {
                        
                        @Override
                        public void request(long n) {
                            sourcePublisherSubscription.request(n);
                        }
                        
                        @Override
                        public void cancel() {
                            sourcePublisherSubscription.cancel();
                        }
                    });
                }
                
            });
    }
}

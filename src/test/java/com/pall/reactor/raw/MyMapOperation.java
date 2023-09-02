package com.pall.reactor.raw;

import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MyMapOperation<T, V> implements Publisher<V> {

    private Publisher<T> sourcePublisher;
    private Function<T, V> mapper = null;
    
    public MyMapOperation(Publisher<T> sourcePublisher, Function<T, V> mapper) {
        super();
        this.sourcePublisher = sourcePublisher;
        this.mapper = mapper;
    }


    @Override
    public void subscribe(Subscriber<? super V> targetSubscriptionSubscriber) {
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
                    targetSubscriptionSubscriber.onNext(mapper.apply(element));
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

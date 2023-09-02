package com.pall.reactor.raw;

import java.util.Objects;
import java.util.function.Function;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

//Almost an exact copy of MyMapOperation but this class extends from MyFlux
//so operations can be chained
public class MyMapFlux<I, O> extends MyFlux<O> {

    private MyFlux<I> sourcePublisher;
    private Function<I, O> mapper;

    public MyMapFlux(MyFlux<I> sourcePublisher, Function<I, O> mapper) {
        super();
        this.sourcePublisher = sourcePublisher;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super O> targetSubscriptionSubscriber) {
        Objects.requireNonNull(sourcePublisher, "Source Publisher has not been set in Operation")
                .subscribe(new Subscriber<I>() {

                    @Override
                    public void onComplete() {
                        targetSubscriptionSubscriber.onComplete();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        targetSubscriptionSubscriber.onError(throwable);

                    }

                    @Override
                    public void onNext(I element) {
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

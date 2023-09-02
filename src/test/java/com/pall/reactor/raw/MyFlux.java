package com.pall.reactor.raw;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

//Very rough Flux to get a basic understanding on how the Flux API works
public abstract class MyFlux<T> implements Publisher<T> {

    public static <T> MyFlux<T> create(Publisher<T> publisher) {
        return new MyCreateFlux<T>(publisher);
    }
    
    public <V> MyFlux<V> map(Function<T, V> mapper){
        return new MyMapFlux<T, V>(this, mapper);
    }
    
    @Override
    public abstract void subscribe(Subscriber<? super T> s);

}

package com.pall.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class CreatingASequence {
    
    @Test
    void fromOptional() throws Exception {
        //Optional with value
        StepVerifier.create(
                Mono.justOrEmpty(Optional.ofNullable("Optional")))
            .expectNext("Optional")
            .verifyComplete();
        
        //Optional with null
        StepVerifier.create(
                Mono.justOrEmpty(Optional.ofNullable(null)))
            .verifyComplete();
    }
    
    @Test
    void fromPotentiallyNullData() throws Exception {
        //With data
        StepVerifier.create(
                Mono.justOrEmpty("Data"))
            .expectNext("Data")
            .verifyComplete();
        
        //With null
        StepVerifier.create(
                Mono.justOrEmpty(null))
            .verifyComplete();
    }
    
    @Test
    void fromSupplier() throws Exception {
        StepVerifier.create(
                Mono.fromSupplier(() -> "Data"))
            .expectNext("Data")
            .verifyComplete();
    }
    
    @Test
    void fromRunnable() throws Exception {
        AtomicBoolean hasRun = new AtomicBoolean();
        StepVerifier.create(
                Mono.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        hasRun.set(true);
                    }
                }))
            .then(() -> assertTrue(hasRun.get()))
            .verifyComplete();
    }
    
    @Test
    void fromCallable() throws Exception {
        StepVerifier.create(
                Mono.fromCallable(new Callable<String>() {
                    @Override
                    public String call() {
                        return "Data";
                    }
                }))
            .expectNext("Data")
            .verifyComplete();
    }
    
    @Test
    void fromCompletableFuture() throws Exception {
        StepVerifier.create(
                Mono.fromFuture(CompletableFuture.completedFuture("Data")))
            .expectNext("Data")
            .verifyComplete();
    }
    
    @Test
    void fromDataItems() throws Exception {
        StepVerifier.create(
                Flux.just("1", "2"))
            .expectNext("1", "2")
            .verifyComplete();
    }
    
    @Test
    void fromArray() throws Exception {
        StepVerifier.create(
                Flux.fromArray(new String[]{"1","2"}))
            .expectNext("1", "2")
            .verifyComplete();
    }
    
    @Test
    void fromCollectionOrIterable() throws Exception {
        StepVerifier.create(
                Flux.fromIterable(List.of("1","2")))
            .expectNext("1", "2")
            .verifyComplete();
    }
    
    @Test
    void fromRangeOfIntegers() throws Exception {
        StepVerifier.create(
                Flux.range(1,3))
            .expectNext(1, 2, 3)
            .verifyComplete();
    }
    
    @Test
    void fromStream() throws Exception {
        StepVerifier.create(
                Flux.fromStream(List.of("1","2").stream()))
            .expectNext("1", "2")
            .verifyComplete();
    }
    
    @Test
    void noItems() throws Exception {
        StepVerifier.create(
                Mono.empty())
            .verifyComplete();
    }
    
    @Test
    void neverDoesAnything() throws Exception {
        StepVerifier.create(
                Flux.never())
            .expectNoEvent(Duration.ofMillis(100))
            .thenCancel();
    }
    
    @Test
    void atSubscriptionTime() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Mono<Integer> defer = Mono.defer(() -> Mono.just(counter.incrementAndGet()));
        
        StepVerifier.create(
                defer)
            .expectNext(1)
            .verifyComplete();
        
        StepVerifier.create(
                defer)
            .expectNext(2)
            .verifyComplete();
        
        //If sequence is not deferred then the value is defined once when the 
        //sequence is created and does not change when the sequence is subscribed
        //to
        counter.set(0);
        
        Mono<Integer> notDeferred = Mono.just(counter.incrementAndGet());
        
        StepVerifier.create(
                notDeferred)
            .expectNext(1)
            .verifyComplete();
        
        StepVerifier.create(
                notDeferred)
            .expectNext(1)
            .verifyComplete();
        
        
    }
    
    @Test
    void programmaticallyCreateASequence() throws Exception {
        
        final int INITIAL_STATE = 0;
        
        StepVerifier.create(Flux.generate(() -> INITIAL_STATE, 
                (state, sink) -> {
                    
                    if (state == 3) {
                        sink.complete();
                    } else {
                        sink.next(state);
                    }
                    
                    return ++state;
                })
            )
            .expectNext(0, 1, 2)
            .verifyComplete();
    }
    
    @Test
    void programmaticallyCreateASequence_WithCleanup() throws Exception {
        final AtomicInteger finalState = new AtomicInteger(); 
        
        StepVerifier.create(Flux.generate(AtomicInteger::new, 
                (state, sink) -> {
                    
                    if (state.get() == 3) {
                        sink.complete();
                    } else {
                        sink.next(state.getAndIncrement());
                    }
                    
                    return state;
                }, 
                (state) -> finalState.set(state.get()))
            )
            .expectNext(0, 1, 2)
            .verifyComplete();
        
        assertEquals(3, finalState.get());
    }

}

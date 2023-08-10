package com.pall.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;

public class HandlingErrors {
    @Test
    void simpleErrorSequence() throws Exception {
        Flux<Object> flux = Flux.error(new RuntimeException("ERROR"));

        StepVerifier.create(flux)
            .expectErrorMessage("ERROR")
            .verify();
    }
    
    @Test
    void replaceCompletionOfSuccessfulSequenceWithError() throws Exception {
        //Flux - using static concat method
        StepVerifier.create(
                Flux.concat(Flux.just(1, 2, 3), Flux.error(RuntimeException::new)))
            .expectNext(1, 2, 3)
            .expectError()
            .verify();
        
        //Flux - using concatWith Operation
        StepVerifier.create(
                Flux.just(1, 2, 3).concatWith(Flux.error(RuntimeException::new)))
            .expectNext(1, 2, 3)
            .expectError()
            .verify();
        
        //Mono - using static concat method. Note: Mono does not have static concat method
        StepVerifier.create(
                Mono.just(1).concatWith(Mono.error(RuntimeException::new)))
        .expectNext(1)
        .expectError()
        .verify();
    }
    
    @Test
    void throwingTimeOutException() throws Exception {
        StepVerifier.withVirtualTime(() ->
                Flux.interval(
                    Duration.ofMinutes(1), // delay
                    Duration.ofMinutes(3)  // interval is greater then timeout for force error
                ).timeout(Duration.ofMinutes(2)))
            .thenAwait(Duration.ofMinutes(1))
            .expectNext(0l)
            .thenAwait(Duration.ofMinutes(2))
            .expectError(TimeoutException.class)
            .verify();    
    }
    
    @Test
    void tryCatchSequence_NoErrorHandling() throws Exception {
        PublisherProbe<Integer> postErrorPublisherProbe = PublisherProbe.of(Flux.just(3));
        
        StepVerifier.create(Flux.just(1, 2)
                .concatWith(Mono.error(RuntimeException::new))
                .concatWith(postErrorPublisherProbe.flux()))
            .expectNext(1, 2)
            .expectError(RuntimeException.class)
            .verify();
        
        postErrorPublisherProbe.assertWasNotSubscribed();
    }
    
    @Test
    void tryCatchSequence_CatchErrorEndReturnAValue() throws Exception {
        Flux<String> erroringFlux = Flux.just("1", "2").concatWith(Flux.error(RuntimeException::new));
        
        StepVerifier.create(erroringFlux.onErrorReturn("ERROR"))
            .expectNext("1", "2", "ERROR")
            .verifyComplete();
    }
    
    @Test
    void tryCatchSequence_CatchErrorAndResumeWithAnotherSequence() throws Exception {
        Flux<String> erroringFlux = Flux.just("1", "2").concatWith(Flux.error(RuntimeException::new));
        
        StepVerifier.<String>create(erroringFlux
                .onErrorResume((e) -> Flux.just("3-fallback", "4-fallback")))
             .expectNext("1", "2", "3-fallback", "4-fallback")
             .verifyComplete();
    }
    
    @Test
    void tryCatchSequence_FinallyIsAlwaysCalled() throws Exception {
        
        //Finally is called after error
        Queue<SignalType> doFinallySignalType = new ConcurrentLinkedQueue<>();
        
        Flux<String> erroringFlux = Flux.just("1", "2").concatWith(Flux.error(RuntimeException::new));
        
        StepVerifier.<String>create(erroringFlux
                .doFinally(doFinallySignalType::add))
            .expectNext("1", "2")
            .expectError()
            .verify();
        
        assertEquals("onError", doFinallySignalType.poll().toString()); 
        
        //Finally is called when there is no error
        doFinallySignalType.clear();
        
        Flux<String> flux = Flux.just("1", "2");
        
        StepVerifier.<String>create(flux
                .doFinally(doFinallySignalType::add))
            .expectNext("1", "2")
            .verifyComplete();
        
        assertEquals("onComplete", doFinallySignalType.poll().toString()); 
    }
    
    @Test
    void onErrorMap_mapExceptionToDifferentException() throws Exception {
        StepVerifier.create(Flux.error(() -> new RuntimeException("ORIGINAL EXCEPTION"))
                .onErrorMap(e -> new Exception("MAPPED EXCEPTION")))
            .expectErrorSatisfies(e -> {
                assertEquals(Exception.class, e.getClass());
                assertEquals("MAPPED EXCEPTION", e.getMessage());
            })
            .verify();
        
    }
    
    @Test
    void onErrorMap_mapExceptionOfTypeToDifferentException() throws Exception {
        StepVerifier.create(Flux.error(() -> new RuntimeException("ORIGINAL EXCEPTION"))
                .onErrorMap(RuntimeException.class, e -> new Exception("MAPPED EXCEPTION")))
            .expectErrorSatisfies(e -> {
                assertEquals(Exception.class, e.getClass());
                assertEquals("MAPPED EXCEPTION", e.getMessage());
            })
            .verify();
       
        //Exception will not be mapped as it is a different type
        StepVerifier.create(Flux.error(() -> new Exception("ORIGINAL EXCEPTION"))
                .onErrorMap(RuntimeException.class, e -> new Exception("MAPPED EXCEPTION")))
            .expectErrorSatisfies(e -> {
                assertEquals(Exception.class, e.getClass());
                assertEquals("ORIGINAL EXCEPTION", e.getMessage());
            })
            .verify();
    }
    
    @Test
    void onErrorMap_mapExceptionBasedOnPredicate() throws Exception {
        //Map as predicate is satisfied
        StepVerifier.create(Flux.error(() -> new RuntimeException("ORIGINAL EXCEPTION"))
                .onErrorMap(e -> e instanceof RuntimeException, e -> new Exception("MAPPED EXCEPTION")))
            .expectErrorSatisfies(e -> {
                assertEquals(Exception.class, e.getClass());
                assertEquals("MAPPED EXCEPTION", e.getMessage());
            })
            .verify();
       
        //Exception will not be mapped as predicate not satisfied
        StepVerifier.create(Flux.error(() -> new Exception("ORIGINAL EXCEPTION"))
                .onErrorMap(e -> e instanceof RuntimeException, e -> new Exception("MAPPED EXCEPTION")))
            .expectErrorSatisfies(e -> {
                assertEquals(Exception.class, e.getClass());
                assertEquals("ORIGINAL EXCEPTION", e.getMessage());
            })
            .verify();
    }
    
    
    
}

package com.pall.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

public class BackPressure {
    @Test
    void backPressure_Drop() throws Exception {
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureDrop()
                ,0)
            .then(() -> tp.next("emitted before request so dropped"))
            .thenRequest(2)
            .then(() -> tp.next("2","3"))
            .expectNext("2", "3")
            .thenRequest(2)
            .then(() -> tp.next(
                    "4","5","not requested so dropped",
                    "not requested so dropped"))
            .expectNext("4","5")
            .then(() -> tp.complete())
            .verifyComplete();
    }
    
    @Test
    void backPressure_Error() throws Exception {
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureError()
                ,0)
            .thenRequest(2)
            .then(() -> tp.next("1","2"))
            .expectNext("1", "2")
            .thenRequest(2)
            .then(() -> tp.next("3","4","not requested so will raise error"))
            .expectNext("3","4")
            .expectError(IllegalStateException.class)
            .verify();
    }
    
    @Test
    void backPressure_Latest() throws Exception {
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureLatest()
                ,0)
            .then(() -> tp.next(
                    "emited before request so dropped", 
                    "item 1 of 2 requested"))
            .thenRequest(2)
            .expectNext("item 1 of 2 requested")
            .then(() -> tp.next(
                    "item 2 of 2 requested", 
                    "not requested so dropped", 
                    "item 1 of 1 requested"))
            .thenRequest(1)
            .expectNext(
                    "item 2 of 2 requested", 
                    "item 1 of 1 requested")
            .then(tp::complete)
            .verifyComplete();
    }
    
    void backPressure_BufferUnbounded() throws Exception {
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureBuffer()
                ,0)
            .then(() -> tp.next(
                    "buffered until requested", 
                    "buffered until requested"))
            .thenRequest(2)
            .expectNext(
                    "buffered until requested",
                    "buffered until requested")
            .then(tp::complete)
            .verifyComplete();
    }
    
    void backPressure_BufferMaxSize() throws Exception {
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureBuffer(2)
                ,0)
            .then(() -> tp.next(
                    "buffered until requested", 
                    "buffered until requested"))
            .thenRequest(2)
            .expectNext(
                    "buffered until requested",
                    "buffered until requested")
            .then(() -> tp.next(
                    "buffered until requested", 
                    "buffered until requested",
                    "over buffer size so exception raised"))
            .expectNext(
                    "buffered until requested",
                    "buffered until requested")
            .expectError(IllegalStateException.class)
            .verify();
    }
    
    @Test
    void backPressure_BufferOverflowStrategy() throws Exception {
        Queue<String> overflowQueue = new ConcurrentLinkedQueue<>();
        TestPublisher<String> tp = TestPublisher.create();
        
        StepVerifier.create(tp.flux()
                .onBackpressureBuffer(2, overflowQueue::add)
                ,0)
            .then(() -> tp.next(
                    "buffered until requested", 
                    "buffered until requested"))
            .thenRequest(2)
            .expectNext(
                    "buffered until requested",
                    "buffered until requested")
            .then(() -> tp.next(
                    "buffered until requested", 
                    "buffered until requested",
                    "greater than buffer size so trigger overflow and raise exception",
                    "downstream has raised exception so item lost"))
            .thenRequest(2)
            .expectNext(
                    "buffered until requested",
                    "buffered until requested")
            .expectError(IllegalStateException.class)
            .verify();
            
        assertEquals("greater than buffer size so trigger overflow and raise exception", overflowQueue.poll());
    }
    
    @Disabled
    void backPressure_BufferOverflowStrategyWithTTL() throws Exception {
        Queue<Long> overflowQueue = new ConcurrentLinkedQueue<>();
        
        StepVerifier.withVirtualTime(() -> 
            Flux.interval(Duration.ofMinutes(1)).onBackpressureBuffer(Duration.ofMinutes(10), 2, overflowQueue::add), 0)   
            .thenAwait(Duration.ofMinutes(2))
            .thenRequest(2)
            .expectNext(0l, 1l)
            .thenAwait(Duration.ofMinutes(4))
            .thenRequest(2)
            .expectNext(4l, 5l)
            .thenAwait(Duration.ofMinutes(4))
            .thenRequest(2)
            .expectNext(8l, 9l)
            .thenCancel()
            .verify();
        
        assertEquals(6, overflowQueue.size());
        assertEquals(2, overflowQueue.poll());
        assertEquals(3, overflowQueue.poll());
    }
    
    @Test
    void backPressure_BufferOverflowTTLWithEviction() throws Exception {
        
        //No requests
        Queue<Long> evictionQueue = new ConcurrentLinkedQueue<>();
        
        StepVerifier.withVirtualTime(() -> 
            Flux.interval(Duration.ofMinutes(1)).onBackpressureBuffer(Duration.ofMinutes(2), 2, evictionQueue::add), 0)   
            .thenAwait(Duration.ofMinutes(4))
            .thenCancel()
            .verify();
        
        assertIterableEquals(List.of(0l, 1l, 2l, 3l), evictionQueue, "All ticks should be bufferred as there has been no requests");
        
        //With Requests
        evictionQueue.clear();
        
        StepVerifier.withVirtualTime(() -> 
            Flux.interval(Duration.ofMinutes(1)).onBackpressureBuffer(Duration.ofMinutes(2), 2, evictionQueue::add), 0)   
                .then(() -> assertIterableEquals(List.of(), evictionQueue, "onBufferEviction should not have been called as no time has passed"))
                .thenAwait(Duration.ofMinutes(3))
                .then(() -> assertIterableEquals(List.of(0l), evictionQueue, "TTL passed, most resent tick [1] should be in buffer and oldest tick [0] should be evicted"))
                .thenRequest(1)
                .expectNext(1l) //buffer is now empty and ttl is reset
                .thenAwait(Duration.ofMinutes(2))
                .then(() -> assertIterableEquals(List.of(0l, 2l), evictionQueue, "TTL passed, most resent tick [3] should be in buffer and oldest tick [2] should be evicted"))
                .thenRequest(1)
                .expectNext(3l) //buffer is now empty and ttl is reset
                .thenCancel()
                .verify();    
    }
}

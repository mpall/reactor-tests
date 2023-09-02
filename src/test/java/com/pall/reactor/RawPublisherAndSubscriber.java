package com.pall.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.pall.reactor.raw.MyFlux;
import com.pall.reactor.raw.MyMapOperation;
import com.pall.reactor.raw.MyNOOPOperation;
import com.pall.reactor.raw.MyPublisher;
import com.pall.reactor.raw.MySubscriber;

import reactor.test.StepVerifier;

public class RawPublisherAndSubscriber {
    
    @Test
    void publisherNotSubscribedTo() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        Exception e = assertThrows(NullPointerException.class, () -> publisher.next("1"));
        assertEquals("Attempted to publish element prior to publisher being subscribed to", e.getMessage());
    }
    
    @Test
    void publishPriorToElementsBeingRequested() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);
        Exception e = assertThrows(RuntimeException.class, () -> publisher.next("1"));
        assertEquals("More elements published than requested", e.getMessage());
    }
    
    @Test
    void publishSingleElement() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);

        subscriber.request(1);
        publisher.next("1");
        
        assertIterableEquals(List.of("1"), subscriber.getOnNextElements());
    }

    @Test
    void moreElementsPublishedThanRequested() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);

        subscriber.request(1);
        publisher.next("1");
        
        Exception e = assertThrows(RuntimeException.class, () -> publisher.next("2"), "More elements published than requested");
        assertEquals("More elements published than requested", e.getMessage());
    }

    
    @Test
    void publishMultipleElement() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);

        subscriber.request(2);
        publisher.next("1", "2");
        
        assertIterableEquals(List.of("1", "2"), subscriber.getOnNextElements());
        
        subscriber.request(3);
        publisher.next("3", "4", "5");
        
        assertIterableEquals(List.of("1", "2", "3", "4", "5"), subscriber.getOnNextElements());
        
    }
    
    @Test
    void elementsCannotPublishAfterSubscriberHasCancelled() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);
        
        subscriber.request(2);
        publisher.next("1", "2");
        
        subscriber.cancel();
        
        Exception e = assertThrows(RuntimeException.class, () -> publisher.next("3"));
        assertEquals("Publisher cancelled by subscriber", e.getMessage());
    }
    
    @Test
    void publisherCompletes() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);
        
        subscriber.request(2);
        publisher.next("1", "2");
        publisher.complete();
        
        assertTrue(subscriber.isOnComplete());
    }
    
    @Test
    void publisherError() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        MySubscriber<String> subscriber = new MySubscriber<>();
        publisher.subscribe(subscriber);
        
        subscriber.request(2);
        publisher.next("1", "2");
        publisher.error(new RuntimeException("Forced error"));
        
        assertEquals("Forced error", subscriber.getThrowable().getMessage());
    }
    
    @Test
    void confirmThatRawPublisherWorksWithStepVerifyer() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        
        StepVerifier.create(publisher, 2)
            .then(() -> publisher.next("1", "2"))
            .expectNext("1", "2")
            .then(() -> publisher.complete())
            .verifyComplete();
    }
    
    @Test
    void simpleOperation_DoesNothing() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();
        
        MyNOOPOperation<String> operation = new MyNOOPOperation<>(publisher);
        
        StepVerifier.create(operation, 0)
            .thenRequest(2)
            .then(() -> publisher.next("1", "2"))
            .expectNext("1", "2")
            .then(() -> publisher.complete())
            .verifyComplete();
    }
    
    @Test
    void simpleOperation_Map() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();     
        MyMapOperation<String, String> operation = 
                new MyMapOperation<>(publisher, v -> v + "map");
        
        StepVerifier.create(operation, 0)
            .thenRequest(2)
            .then(() -> publisher.next("1", "2"))
            .expectNext("1map", "2map")
            .then(() -> publisher.complete())
            .verifyComplete();
        
    }
    
    @Test
    void fluxLikeAPI_Create() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();     
        
        MyFlux<String> flux = MyFlux.create(publisher);
        
        StepVerifier.create(flux, 0)
            .thenRequest(2)
            .then(() -> publisher.next("1", "2"))
            .expectNext("1", "2")
            .then(() -> publisher.complete())
            .verifyComplete();
    }
    
    
    @Test
    void fluxLikeAPI_Map() throws Exception {
        MyPublisher<String> publisher = new MyPublisher<>();     
        
        //The map flux is an almost exact copy of MyMapOperation to help
        //in understanding how operators are chained
        MyFlux<String> flux = MyFlux.create(publisher)
                .map(v -> v + "_first")
                .map(v -> v + "_second");
        
        StepVerifier.create(flux, 0)
            .thenRequest(2)
            .then(() -> publisher.next("1", "2"))
            .expectNext("1_first_second", "2_first_second")
            .then(() -> publisher.complete())
            .verifyComplete();
        
    }
    
    

}

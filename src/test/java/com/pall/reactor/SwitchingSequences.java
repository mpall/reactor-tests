package com.pall.reactor;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

public class SwitchingSequences {
    @Test
    void switchMap() throws Exception {
        StepVerifier.create(Flux.just("1", "2").switchMap(v -> Flux.just(format("main item [%s] sub item [1]", v), format("main item [%s] sub item [2]", v))))
            .expectNext(
                    "main item [1] sub item [1]", 
                    "main item [1] sub item [2]", 
                    "main item [2] sub item [1]", 
                    "main item [2] sub item [2]")
            .verifyComplete();
    }
    
    @Test
    void switchOnFirst() throws Exception {
        StepVerifier.create(Flux.just("1", "2").switchOnFirst((signal, fluxOfValue) -> {
                return fluxOfValue.zipWith(Mono.just(signal));
             }))
            .assertNext(t -> {
                assertEquals("1", t.getT1());
                assertEquals("1", t.getT2().get());
                assertEquals("onNext", t.getT2().getType().toString());
            })
            .verifyComplete();
    }
    
    @Test
    void switchOnNext_ForwardDataToNewPublisherWhenPublisherIsAvailableInSource() throws Exception {
        TestPublisher<String> tp1 = TestPublisher.create();
        TestPublisher<String> tp2 = TestPublisher.create();
        TestPublisher<TestPublisher<String>> sourcePublishers = TestPublisher.create(); 

        StepVerifier.create(Flux.switchOnNext(
                        sourcePublishers.flux()))
        .then(() -> sourcePublishers.next(tp1))
        .then(() -> tp1.next("tp1_1", "tp1_2"))
        .expectNext("tp1_1", "tp1_2")
        .then(() -> sourcePublishers.next(tp2)) // Switch sequence to new flux (tp2) even though old flux (tp1) has not ended
        .then(() -> tp1.next("tp1_3", "tp1_4")) // Not published to main flux as new flux has been added
        .then(() -> tp2.next("tp2_1", "tp2_2"))
        .expectNext("tp2_1", "tp2_2")
        .then(() -> tp1.complete())
        .then(() -> tp2.complete())
        .then(() -> sourcePublishers.complete())
        .verifyComplete();
        
    }
}

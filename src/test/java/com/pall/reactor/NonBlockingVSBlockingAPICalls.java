package com.pall.reactor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.String.format;
import static org.springframework.web.reactive.function.client.WebClient.create;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

//The following is a bad example of test coding as it has fixed delays in it.

@WireMockTest
public class NonBlockingVSBlockingAPICalls {

    @BeforeEach
    public void stubs() {
        stubFor(get(urlEqualTo("/fast"))
            .willReturn(aResponse()
                 .withStatus(200)
                 .withBody("Fast Response"))
            );

        stubFor(get(urlEqualTo("/slow"))
            .willReturn(aResponse()
                .withFixedDelay(500)
                .withStatus(200)
                .withBody("Slow Response"))
            );
    }

    @Test
    @DisplayName("Non-blocking API call does not require additional threads to serve the fastest responses first")
    public void testNonBlocking(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given a sequence where the slowest API is called first
        Flux<String> flux = Flux.just(
            "/slow", 
            "/fast", 
            "/fast")
            .flatMap(uri -> 
                    create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                    .get()
                    .uri(uri)
       // When a non-blocking API call is made
                    .retrieve()
                    .bodyToFlux(String.class));

        // Then the fastest responses are served first as the call is non-blocking
        StepVerifier.create(flux)
            .expectNext("Fast Response", 
                        "Fast Response", 
                        "Slow Response")
            .verifyComplete();
    }

    @Test
    @DisplayName("Blocking API client does NOT serve the fasted response first")
    public void testBlockingCall(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given a sequence where the slowest API is called first
        Flux<String> flux = Flux.just(
            "/slow", 
            "/fast", 
            "/fast")
            .map(uri -> 
                create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                .get()
                .uri(uri)
        // When a blocking API call is made
                .retrieve()
                .bodyToFlux(String.class)
                .blockFirst());

        // Then the slowest response is still served first
        StepVerifier.create(flux)
            .expectNext("Slow Response", 
                        "Fast Response", 
                        "Fast Response")
            .verifyComplete();

    }

    @Test
    @DisplayName("If additional threads are spun up then Blocking API client can serve the fasted response first")
    public void testBlockingCall_BoundedElastic(WireMockRuntimeInfo wmRuntimeInfo) {
        // Given a sequence where the slowest API is called first
        Flux<String> flux = Flux.just(
            "/slow", 
            "/fast", 
            "/fast")
            .flatMap(uri -> Mono.just(uri)
        // And blocking call can run in additional threads
                .publishOn(Schedulers.boundedElastic())
                .map(u -> 
                    create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort())).get().uri(u)
        // When a blocking call is made
                .retrieve()
                .bodyToFlux(String.class)
                .blockFirst()));

        // Then the fastest responses are served first
        StepVerifier.create(flux)
            .expectNext("Fast Response", 
                        "Fast Response", 
                        "Slow Response")
            .verifyComplete();
    }

}

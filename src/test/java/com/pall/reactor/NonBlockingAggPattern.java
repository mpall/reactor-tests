package com.pall.reactor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.springframework.web.reactive.function.client.WebClient.create;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@WireMockTest
public class NonBlockingAggPattern {

    
 // Scenario: A customer needs enriched information based on tax and mot information which are all provided be separate services
 // When The customer requests the data for the vehicle registration 
 // Then provide in a single response in the shortest possible time.

    @BeforeEach
    public void stubs() {
        stubFor(get(urlPathEqualTo("/dvsa/mot"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("MOT for {{request.query.reg}}")
                .withTransformers("response-template")));
        
        stubFor(get(urlPathEqualTo("/dvla/tax"))
            .willReturn(aResponse()
                .withStatus(200)
               .withBody("TAX for {{request.query.reg}}")
               .withTransformers("response-template")));
        
        stubFor(post(urlPathEqualTo("/enrich"))
                .willReturn(aResponse()
                    .withStatus(200)
                   .withBody("Enriched data for {{jsonPath request.body '$.reg'}}. {{jsonPath request.body '$.tax'}}. {{jsonPath request.body '$.mot'}}"   )
                   .withTransformers("response-template")));
        
        
        
    }
    
    
    @Test
    void motOnly(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        Flux<String> flux = Flux.just(
                "/dvsa/mot?reg=T00 AAA"
                ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToFlux(String.class)   
        );
        
        StepVerifier.create(flux)
            .expectNext(
                "MOT for T00 AAA"
                )
            .verifyComplete();
    }
    
    @Test
    void taxOnly(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        
        Flux<String> flux = Flux.just(
                "/dvla/tax?reg=T00 AAA"
                ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToFlux(String.class)   
        );
        
        
        
        StepVerifier.create(flux)
        .expectNext(
            "TAX for T00 AAA"
            )
        .verifyComplete();
    }
    
    
    @Test
    void enrichmentOnly(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        
        Flux<String> flux = Flux.just(
                "/enrich"
                ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                    .post()
                    .uri(uri)
                     .body(fromValue("{\"reg\":\"T00 AAA\",\"mot\":\"MOT for T00 AAA\",\"tax\":\"TAX for T00 AAA\"}"))
                    .retrieve()
                    .bodyToFlux(String.class)   
        );
        
        StepVerifier.create(flux)
            .expectNext("Enriched data for T00 AAA. TAX for T00 AAA. MOT for T00 AAA")
            .verifyComplete();
    
    }
    
    
    
    @Test
    void aggPattern(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        Flux<String> flux = Flux.just(
                "T00 AAA",
                "T11 BBB"
            ).flatMap(reg -> {
                Mono<String> motMono = Mono.just(
                        format("/dvsa/mot?reg=%s", reg)
                        ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                            .get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class));
                
                Mono<String> taxMono = Mono.just(
                        format("/dvla/tax?reg=%s", reg)
                        ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                            .get()
                            .uri(uri)
                            .retrieve()
                            .bodyToMono(String.class)   
                );
                
                Mono<String> enrichedMono = motMono.zipWith(taxMono).flatMap(tuple -> {
                    String motResponse = tuple.getT1();
                    String taxResponse = tuple.getT2();
                    
                    return Mono.just(
                            "/enrich"
                            ).flatMap(uri -> create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))
                                .post()
                                .uri(uri)
                                 .body(fromValue(format("{\"reg\":\"%s\",\"mot\":\"%s\",\"tax\":\"%s\"}", reg, motResponse, taxResponse)))
                                .retrieve()
                                .bodyToMono(String.class)   
                    );
                });
                
              return enrichedMono;  
            });
        
        
        List<String> forceOrderNonBlockingResponse = flux.collectSortedList().block();
        assertEquals("Enriched data for T00 AAA. TAX for T00 AAA. MOT for T00 AAA", forceOrderNonBlockingResponse.get(0));
        assertEquals("Enriched data for T11 BBB. TAX for T11 BBB. MOT for T11 BBB", forceOrderNonBlockingResponse.get(1));
        
    }
    
    
    

}

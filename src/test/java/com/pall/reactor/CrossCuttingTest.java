package com.pall.reactor;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

public class CrossCuttingTest {

	private static final String OPERATOR_KEY = "operator-key";

	//https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Operators.html#lift-java.util.function.BiFunction-
	//https://projectreactor.io/docs/core/release/reference/#hooks-assembly
	
	
	
	@Test
	void addDecoratorToEachOperator() throws Exception {
		AtomicInteger countOfDecoratorOnNextCalls = new AtomicInteger();
		
		Hooks.onEachOperator(OPERATOR_KEY, operatorWrapperCoreSubscriber(countOfDecoratorOnNextCalls));
		

		Flux<String> fluxToLift_3_Operators_2_elements = Flux.just("1", "2")
				.map(v -> v + "A")
				.map(v -> v + "B")
				;

		fluxToLift_3_Operators_2_elements.blockLast();
		
		// On next called 
		//	twice in operator "just" and 
		//  twice in operator "map"
		//  twice in operator "map"
		assertEquals(6, countOfDecoratorOnNextCalls.get());
	}
	
		
	public static <T> Function<? super Publisher<T>, ? extends Publisher<T>> operatorWrapperCoreSubscriber(AtomicInteger countOfDecoratorOnNextCalls) {
		return sourcePub -> {
		    
		    if (!Scannable.from(sourcePub).isScanAvailable()){
		        return sourcePub;
		    }
		    
		    
			Function<? super Publisher<T>, ? extends Publisher<T>> lift = Operators.lift((scannable, sub) -> {
				CoreSubscriber<T> subscriber = new CoreSubscriber<T>() {
					@Override
					public void onSubscribe(Subscription s) {
						sub.onSubscribe(s);
					}

					@Override
					public void onNext(T o) {
						System.out.println(format("Lift Function: Step[%s] onNext [%s]", scannable.stepName(), o));
						countOfDecoratorOnNextCalls.incrementAndGet();
						sub.onNext(o);
					}

					@Override
					public void onError(Throwable t) {
						sub.onError(t);
					}

					@Override
					public void onComplete() {
						sub.onComplete();
					}
				};
				
				return subscriber;
			});
			
			return lift.apply(sourcePub);
		};
	
	}
}

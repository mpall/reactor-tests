package com.pall.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class ConnectableFluxTest {

    
    @Test
    void testAddMultipleSubscribersToAFlux() throws Exception {     
        List<Integer> intList1 = Collections.synchronizedList(new LinkedList<>());
        List<Integer> intList2 = Collections.synchronizedList(new LinkedList<>());
        AtomicBoolean firstSubscriptionSubscribedTo = new AtomicBoolean();
        AtomicBoolean secondSubscriptionSubscribedTo = new AtomicBoolean();
        AtomicBoolean connectedFluxSubscribedTo = new AtomicBoolean();
        
        int itemCount = 10;
        ConnectableFlux<Integer> cf = Flux.range(0, itemCount)
                .doOnSubscribe(s -> connectedFluxSubscribedTo.set(true))
                .publish();
    
        cf.doOnSubscribe(s -> firstSubscriptionSubscribedTo.set(true)).subscribe(i -> intList1.add(i));
        cf.doOnSubscribe(s -> secondSubscriptionSubscribedTo.set(true)).subscribe(i -> intList2.add(i));
        
        assertTrue(firstSubscriptionSubscribedTo.get());
        assertTrue(secondSubscriptionSubscribedTo.get());
        assertFalse(connectedFluxSubscribedTo.get(), "ConnectableFlux is not subscribed to even though downstream Fluxs have been subscribed to");
        
        assertEquals(0, intList1.size());
        assertEquals(0, intList2.size());
        
        cf.connect();
        
        assertEquals(itemCount, intList1.size());
        assertEquals(itemCount, intList2.size());
    }
    
    
    
}

package com.pall.reactor;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;
import reactor.util.function.Tuples;

public class MergingSequences {
    
    @Test
    void concat() throws Exception {
        StepVerifier.create(Flux.concat(
                   Flux.just("seq1", "seq1"), 
                   Flux.just("seq2", "seq2")))
            .expectNext("seq1", "seq1", "seq2", "seq2")
            .verifyComplete();
    }
    
    @Test
    void concatWith() throws Exception {
        StepVerifier.create(
                Flux.just("seq1", "seq1")
                    .concatWith(Flux.just("seq2", "seq2")))
            .expectNext("seq1", "seq1", "seq2", "seq2")
            .verifyComplete();
    }
    
    @Test
    void zip() throws Exception {
        //Even number of published items from both sequences
        StepVerifier.create(Flux.zip(
                Flux.just("seq1", "seq1"), 
                Flux.just("seq2", "seq2")))
         .expectNext(
                Tuples.of("seq1", "seq2"), 
                Tuples.of("seq1", "seq2"))
         .verifyComplete();
        
        
      //Uneven number of published items from sequences. Additional items dropped
        StepVerifier.create(Flux.zip(
                Flux.just("seq1", "seq1"), 
                Flux.just("seq2", "seq2", "seq2", "seq2")))
         .expectNext(
                Tuples.of("seq1", "seq2"), 
                Tuples.of("seq1", "seq2"))
         .verifyComplete();
    }
    
    @Test
    void zipWith_RequiresEvenNumberOfEmissionsOnEachSequence() throws Exception {
        TestPublisher<String> seq1 = TestPublisher.create();
        TestPublisher<String> seq2 = TestPublisher.create();
        
        
        StepVerifier.create(seq1.flux().zipWith(seq2.flux()))
            .then(() -> seq1.next("seq1_item1"))
            .then(() -> seq2.next("seq2_item1"))
            .expectNext(
                Tuples.of("seq1_item1", "seq2_item1"))
            .then(() -> seq2.next("seq2_item2"))
            .then(() -> seq2.next("seq2_item3"))
            .then(() -> seq2.next("seq2_item4"))
            .then(() -> seq2.complete())
            //Even though seq2 is complete the main sequence will not end until there are an even number of emissions
            .then(() -> seq1.next("seq1_item2"))
            .then(() -> seq1.next("seq1_item3"))
            .then(() -> seq1.next("seq1_item4"))
            .expectNext(
                Tuples.of("seq1_item2", "seq2_item2"),
                Tuples.of("seq1_item3", "seq2_item3"),
                Tuples.of("seq1_item4", "seq2_item4"))
            //seq1 did not need to complete as an even number of emissions have been reached triggering the end of the main sequence
            .verifyComplete();
    }
    
    
    @Test
    void expandBreadthFirst() throws Exception {
        Node node1 = new Node(
                "level1_1",
                List.of(
                   new Node("level1_1:level2_1"), 
                   new Node("level1_1:level2_2"))
                );
       
       Node node2 = new Node(
               "level1_2",
               List.of(
                  new Node("level1_2:level2_1", List.of(
                                          new Node("level1_2:level2_1:level3_1"))), 
                  new Node("level1_2:level2_2"))
                );
        
        StepVerifier.create(
            Flux.just(node1, node2)
                .expand(n -> Flux.fromIterable(n.getChildNodes()))
                .map(Node::getNodeName))
        .expectNext( 
                "level1_1", 
                "level1_2", 
                "level1_1:level2_1", 
                "level1_1:level2_2", 
                "level1_2:level2_1", 
                "level1_2:level2_2",
                "level1_2:level2_1:level3_1"
                )
        .verifyComplete();
    }
    
    @Test
    public void expandDepthFirst() throws Exception {
        Node node1 = new Node(
                 "level1_1",
                 List.of(
                    new Node("level1_1:level2_1"), 
                    new Node("level1_1:level2_2"))
                 );
        
        Node node2 = new Node(
                "level1_2",
                List.of(
                   new Node("level1_2:level2_1", List.of(
                                           new Node("level1_2:level2_1:level3_1"))), 
                   new Node("level1_2:level2_2"))
                 );
 
        
        StepVerifier.create(
            Flux.just(node1, node2)
                .expandDeep(n -> Flux.fromIterable(n.getChildNodes()))
                .map(Node::getNodeName))
        .expectNext(
                "level1_1", 
                "level1_1:level2_1", 
                "level1_1:level2_2",
                "level1_2",
                "level1_2:level2_1", 
                "level1_2:level2_1:level3_1",
                "level1_2:level2_2"
                )
        .verifyComplete();
    }
    
    private class Node {
        private List<Node> childNodes = new LinkedList<>();
        private String nodeName;
        
        public Node(String nodeName) {
            this.nodeName = nodeName;
        }
        
        public Node(String nodeName, List<Node> childNodes) {
            this.nodeName = nodeName;
            this.childNodes = childNodes;
        }
        
        public String getNodeName() {
            return nodeName;
        }
        
        public List<Node> getChildNodes() {
            return childNodes;
        }
    }

}

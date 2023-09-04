# reactor-tests

An attempt to better understand Reactor.

WARNING: This repo does not represent any good practice in testing or Java development.

# Tests
## Blocking vs non-Blocking
Demo of:
* how slow IO does not block a sequence when code is non-blocking.
* how blocking code does block a sequence.
* how to make blocking code run on a seperate thread to not block a sequence. 

[NonBlockingVSBlockingAPICalls](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/NonBlockingVSBlockingAPICalls.java)

## Base understanding of core reactive interfaces
Attempt to understand Publisher and Subscriber interfaces by creating basic implementations. Sample implementations include [MyPublisher](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/raw/MyPublisher.java), [MySubscriber](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/raw/MySubscriber.java) and [MyFlux](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/raw/MyFlux.java). Test are in [RawPublisherAndSubscriber](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/RawPublisherAndSubscriber.java)

## Add decorator to every operation
This pattern could be used as an alternative to threadlocal for setting global logging values on each onNext request.
[CrossCuttingTest](https://github.com/mpall/reactor-tests/blob/master/src/test/java/com/pall/reactor/CrossCuttingTest.java)

## The rest
The rest of the tests have been created off the back of the [projectreactor reference](https://projectreactor.io/docs/core/release/reference/) to reenforce how reactor works.

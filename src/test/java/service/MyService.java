package service;

import org.reactivestreams.Subscription;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyService {
  StateOrientedService stateOrientedService = new StateOrientedService();

  public Flux<Chunk> unlimit() {
    return Flux.interval(Duration.ofMillis(500))
        .doOnRequest(count -> System.out.println("request of " + count))
        .doOnNext(idx -> System.out.println("[doOnNext] idx:" + idx))
        .map(Chunk::getInstance);
  }

  public Flux<Chunk> limit(int itemCount) {
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    AtomicInteger subscribed = new AtomicInteger();
    return Flux.interval(Duration.ofMillis(500))
        .doOnRequest(count -> System.out.println("request of " + count))
        .doOnSubscribe(subscription -> {
          subscriptionRef.set(subscription);
          //                    subscription.request(1);
        })
        .map(Chunk::getInstance)
        .doOnNext(chunk -> {
          System.out.println("[doOnNext] idx:" + chunk.getLastKey());
          if (subscribed.addAndGet(chunk.getCount()) >= itemCount) {
            subscriptionRef.get().cancel();
          }
          //                    else {
          //                        subscriptionRef.get().request(1);
          //                    }
        });
  }

  public Flux<Chunk> limitUsingUnlimit(int itemCount) {
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    AtomicInteger subscribed = new AtomicInteger();

    return unlimit()
        .doOnSubscribe(subscriptionRef::set)
        .doOnNext(chunk -> {
          if (subscribed.addAndGet(chunk.getCount()) >= itemCount) {
            subscriptionRef.get().cancel();
          }
        });
  }

  //    public Flux<Integer> recursive(int key) {
  //      final int[] nextKey = {0};
  //      Flux<Integer> inner = inner(key);
  //      inner.doOnNext(retVal -> nextKey[0] = retVal);
  //
  //      return inner.concatWith(recursive(nextKey[0]));
  //    }
  //
  private Mono<Integer> inner(int key) {
    return Mono.just(key + 1).delayElement(Duration.ofSeconds(1));
  }

  public Flux<Integer> generate(int key) {
    return Flux.generate(
        () -> key,
        (state, sink) -> {
          System.out.println(Thread.currentThread());
          Integer nextKey = inner(state).block();
          sink.next(nextKey);
          //            if (nextKey > 10) {
          //              sink.complete();
          //            }
          return nextKey;
        });
  }

  public Flux<Integer> concatMap() {
    return Flux.range(1, Integer.MAX_VALUE)
        .concatMap(i -> stateOrientedService.emit(), 32);
  }

  public Mono<List<Chunk>> limitWithEmitterProcessor(int itemCount) {
    EmitterProcessor<Boolean> emitterProcessor = EmitterProcessor.create();
    AtomicInteger subscribed = new AtomicInteger();

    return Flux.interval(Duration.ofMillis(500))
        .doOnRequest(count -> System.out.println("request of " + count))
        .doOnNext(idx -> System.out.println("[doOnNext] idx:" + idx))
        .map(Chunk::getInstance)
        .doOnNext(chunk -> {
          System.out.println("[doOnNext] idx:" + chunk.getLastKey());
          if (subscribed.addAndGet(chunk.getCount()) >= itemCount) {
            emitterProcessor.onNext(true);
            emitterProcessor.onComplete();
          }
        })
        .takeUntilOther(emitterProcessor)
        .collectList();
  }
}

package service;

import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MyService {
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
}

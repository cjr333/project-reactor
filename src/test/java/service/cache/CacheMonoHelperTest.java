package service.cache;

import org.junit.Test;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CacheMonoHelperTest {
  @Test
  public void cacheMonoHelperTest() throws InterruptedException {
    CacheMonoHelper<Integer, Long> cacheMonoHelper = CacheMonoHelper.<Integer, Long>builder()
        .supplier((key) -> {
          System.out.println("Call supplier");    // 캐시된 경우 호출되면 안됨
          return Mono.just(System.currentTimeMillis() + key);
        })
        .expire(Duration.ofSeconds(10))
        .maxEntry(2)
        .build();

    cacheMonoHelper.get(1).subscribe(logTimestampWithExpect("cache entry first"));

    Thread.sleep(1000);

    // Cache Ok
    cacheMonoHelper.get(1).subscribe(logTimestampWithExpect("same cached ts"));   // expect same ts

    // Cache expire
    Thread.sleep(10000);

    cacheMonoHelper.get(1).subscribe(logTimestampWithExpect("new ts because of expire"));   // expect new ts

    // delete cache because of max entry size
    cacheMonoHelper.get(2).subscribe();   // cache for key 2
    cacheMonoHelper.get(3).subscribe();   // cache for key 3

    Thread.sleep(1000);

    cacheMonoHelper.get(1).subscribe(logTimestampWithExpect("new ts because of max entry"));   // expect new ts

    Thread.sleep(3000);
  }

  private Consumer<Long> logTimestampWithExpect(String expect) {
    return ts -> System.out.println(String.format("[%d] %s", ts, expect));
  }

  @Test
  public void cacheMonoTest() throws InterruptedException {
    AtomicInteger index = new AtomicInteger(0);
//    CacheMonoHelper<Integer> cacheMonoHelper = CacheMonoHelper.<Integer>builder().supplier(() -> Mono.just(1)).build();

    AtomicReference<Context> storeRef = new AtomicReference<>(Context.empty());

    Mono<Integer> first = CacheMono
        .lookup(k -> Mono.justOrEmpty(storeRef.get().<Integer>getOrEmpty(k))
                .map(Signal::next),
            1)
        .onCacheMissResume(Mono.fromSupplier(() -> 1 / index.getAndIncrement()))
        .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
            storeRef.updateAndGet(ctx -> ctx.put(k, sig.get()))))
        .doOnNext(integer -> System.out.println("Subscribe: " + integer))
        .doOnError((throwable) -> System.out.println("Error: " + throwable.getMessage()));

    first.subscribe();

//    Mono.fromSupplier(() -> 1 / index.getAndIncrement())
//        .doOnNext(integer -> System.out.println("Subscribe: " + integer))
//        .doOnError((throwable) -> System.out.println("Error: " + throwable.getMessage()))
//        .subscribe();
//
//    Mono.fromSupplier(() -> 1 / index.getAndIncrement())
//        .doOnNext(integer -> System.out.println("Subscribe: " + integer))
//        .doOnError((throwable) -> System.out.println("Error: " + throwable.getMessage()))
//        .subscribe();

    Mono<Integer> second = CacheMono
        .lookup(k -> Mono.justOrEmpty(storeRef.get().<Integer>getOrEmpty(k))
                .map(Signal::next),
            1)
        .onCacheMissResume(Mono.fromSupplier(() -> 1 / index.getAndIncrement()))
        .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
            storeRef.updateAndGet(ctx -> ctx.put(k, sig.get()))))
        .doOnNext(integer -> System.out.println("Subscribe: " + integer))
        .doOnError((throwable) -> System.out.println("Error: " + throwable.getMessage()));
    second.subscribe();

    Mono<Integer> third = CacheMono
        .lookup(k -> Mono.justOrEmpty(storeRef.get().<Integer>getOrEmpty(k))
                .map(Signal::next),
            1)
        .onCacheMissResume(Mono.fromSupplier(() -> 1 / index.getAndIncrement()))
        .andWriteWith((k, sig) -> Mono.fromRunnable(() ->
            storeRef.updateAndGet(ctx -> ctx.put(k, sig.get()))))
        .doOnNext(integer -> System.out.println("Subscribe: " + integer))
        .doOnError((throwable) -> System.out.println("Error: " + throwable.getMessage()));
    third.subscribe();

    Thread.sleep(5000);
  }
}

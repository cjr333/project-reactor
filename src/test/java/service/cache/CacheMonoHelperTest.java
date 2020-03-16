package service.cache;

import org.junit.Test;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CacheMonoHelperTest {
  @Test
  public void success() throws InterruptedException {
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

  @Test
  public void errorCacheExpire() throws InterruptedException {
    final long startTs = System.currentTimeMillis();
    CacheMonoHelper<Integer, Long> cacheMonoHelper = CacheMonoHelper.<Integer, Long>builder()
        .supplier((key) -> {
          System.out.println("Call supplier");    // 캐시된 경우 호출되면 안됨
          if (System.currentTimeMillis() - startTs < 1000) {
            return Mono.error(new RuntimeException("test exception"));
          } else {
            return Mono.just(System.currentTimeMillis() + key);
          }
        })
        .expire(Duration.ofSeconds(5))
        .errorExpire(Duration.ofSeconds(2))
        .maxEntry(2)
        .build();

    StepVerifier.create(cacheMonoHelper.get(1))
        .expectErrorMessage("test exception")
        .verify();

    Thread.sleep(1000);

    // Error Cached
    StepVerifier.create(cacheMonoHelper.get(1))
        .expectErrorMessage("test exception")
        .verify();

    Thread.sleep(2000);

    // Error Cached expired
    StepVerifier.create(cacheMonoHelper.get(1))
        .expectNextCount(1)
        .expectComplete()
        .verify();
  }

  @Test
  public void notCacheForError() throws InterruptedException {
    final long startTs = System.currentTimeMillis();
    final AtomicInteger index = new AtomicInteger(0);
    CacheMonoHelper<Integer, Long> cacheMonoHelper = CacheMonoHelper.<Integer, Long>builder()
        .supplier((key) -> {
          System.out.println("Call supplier");    // 캐시된 경우 호출되면 안됨
          if (System.currentTimeMillis() - startTs < 1000) {
            return Mono.error(new RuntimeException("test exception" + index.getAndIncrement()));
          } else {
            return Mono.just(System.currentTimeMillis() + key);
          }
        })
        .expire(Duration.ofSeconds(5))
        .errorExpire(Duration.ofSeconds(0))
        .maxEntry(2)
        .build();

    StepVerifier.create(cacheMonoHelper.get(1))
        .expectErrorMessage("test exception0")
        .verify();

    // Error not cached
    StepVerifier.create(cacheMonoHelper.get(1))
        .expectErrorMessage("test exception1")
        .verify();

    Thread.sleep(1000);

    // success
    StepVerifier.create(cacheMonoHelper.get(1))
        .expectNextCount(1)
        .expectComplete()
        .verify();
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

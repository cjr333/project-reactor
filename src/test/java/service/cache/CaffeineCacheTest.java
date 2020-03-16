package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CaffeineCacheTest {
  AtomicInteger value = new AtomicInteger(0);

  @Test
  public void test() throws InterruptedException {
    Integer key = 1;
    Cache<Integer, Mono<Long>> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(100)
        .build();

    Mono<Long> result = cache.getIfPresent(key);
    if (result == null) {
      result = callSimul();
      cache.put(key, result);
    }
    result.subscribe(aLong -> System.out.println("result: " + aLong));

    Thread.sleep(2000);

    result = cache.getIfPresent(key);
    assert result != null;

    result.subscribe(aLong -> System.out.println("result: " + aLong));
    Thread.sleep(10000);
  }

  private Mono<Long> callSimul() {
    return Mono.just(value.getAndIncrement())
        .flatMap(this::innerFlatMap);
  }

  private Mono<Long> innerFlatMap(int integer) {
    return Mono.just(System.currentTimeMillis() * 100 + integer).delayElement(Duration.ofSeconds(2));
  }

  @Test
  public void test2() throws InterruptedException {
    Integer key = 1;
    Cache<Integer, Mono<List<String>>> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(100)
        .build();

    Mono<List<String>> result = cache.getIfPresent(key);
    if (result == null) {
      result = checkInput("2");
      cache.put(key, result);
    }
    result.subscribe(aLong -> System.out.println("result: " + aLong));

    Thread.sleep(2000);

    result = cache.getIfPresent(key);
    assert result != null;

    result.subscribe(aLong -> System.out.println("result: " + aLong));
    Thread.sleep(10000);
  }

  public Flux<String> generateSampleList() {
    System.out.println("generateSampleList");
    return Flux.range(0, 3).map(String::valueOf)
        .onErrorResume(e -> Mono.error(new IllegalArgumentException()));
  }

  public Mono<List<String>> checkInput(String input) {
    return generateSampleList()
        .flatMap(sample -> {
          if (sample.equals(input)){
            return generateSampleList();
          } else {
            return Flux.empty();
          }
        })
        .switchIfEmpty(Mono.error(new IllegalArgumentException()))
        .collectList()
        ;
  }
}

package service.cache;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Consumer;

public class CacheFluxHelperTest {
  @Test
  public void cacheMonoHelperTest() throws InterruptedException {
    CacheFluxHelper<Integer, Long> cacheFluxHelper = CacheFluxHelper.<Integer, Long>builder()
        .supplier((key) -> Flux.interval(Duration.ofMillis(200)).map(aLong -> System.currentTimeMillis() + key).take(3))
        .expire(Duration.ofSeconds(10))
        .maxEntry(2)
        .build();

    cacheFluxHelper.get(1).take(3).subscribe(logTimestampWithExpect("cache entry first"));

    Thread.sleep(1000);

    // Cache Ok
    cacheFluxHelper.get(1).take(2).subscribe(logTimestampWithExpect("same cached ts"));   // expect same ts
    cacheFluxHelper.get(1).take(1).subscribe(logTimestampWithExpect("same cached ts"));   // expect same ts

    // Cache expire
    Thread.sleep(10000);

    cacheFluxHelper.get(1).take(3).subscribe(logTimestampWithExpect("new ts because of expire"));   // expect new ts

    // delete cache because of max entry size
    cacheFluxHelper.get(2).take(3).subscribe();   // cache for key 2
    cacheFluxHelper.get(3).take(3).subscribe();   // cache for key 3

    Thread.sleep(1000);

    cacheFluxHelper.get(1).take(3).subscribe(logTimestampWithExpect("new ts because of max entry"));   // expect new ts

    Thread.sleep(3000);
  }

  @Test
  public void cacheFluxHelperErrorTest() throws InterruptedException {
    final long startTs = System.currentTimeMillis();
    CacheFluxHelper<Integer, Long> cacheFluxHelper = CacheFluxHelper.<Integer, Long>builder()
        .supplier((key) -> {
          System.out.println("Call supplier");    // 캐시된 경우 호출되면 안됨
          if (System.currentTimeMillis() - startTs < 1000) {
            return Flux.range(0, 3).flatMap(integer -> {
              if (integer == 2) {
                return Flux.<Long>error(new RuntimeException("test exception"));
              } else {
                return Flux.just(System.currentTimeMillis() + integer);
              }
            });
          } else {
            return Flux.range(0, 3).map(integer -> System.currentTimeMillis() + integer);
          }
        })
        .expire(Duration.ofSeconds(5))
        .errorExpire(Duration.ofSeconds(2))
        .maxEntry(2)
        .build();

    StepVerifier.create(cacheFluxHelper.get(1))
        .expectNextCount(2)
        .expectErrorMessage("test exception")
        .verify();

    Thread.sleep(1000);

    // Error Cached
    StepVerifier.create(cacheFluxHelper.get(1))
        .expectNextCount(2)
        .expectErrorMessage("test exception")
        .verify();

    Thread.sleep(2000);

    // Error Cached expired
    StepVerifier.create(cacheFluxHelper.get(1))
        .expectNextCount(3)
        .expectComplete()
        .verify();
  }

  private Consumer<Long> logTimestampWithExpect(String expect) {
    return ts -> System.out.println(String.format("[%d] %s", ts, expect));
  }
}

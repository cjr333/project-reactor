package service.cache;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Consumer;

public class CacheFluxHelperTest {
  @Test
  public void cacheMonoHelperTest() throws InterruptedException {
    CacheFluxHelper<Integer, Long> cacheFluxHelper = CacheFluxHelper.<Integer, Long>builder()
        .supplier(() -> Flux.interval(Duration.ofMillis(200)).map(aLong -> System.currentTimeMillis() + aLong).take(3))
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

  private Consumer<Long> logTimestampWithExpect(String expect) {
    return ts -> System.out.println(String.format("[%d] %s", ts, expect));
  }
}

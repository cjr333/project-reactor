package service.cache;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class RequestOperatorTest {
  @Test
  public void test() throws InterruptedException {
    Flux<Long> push = Flux.interval(Duration.ofMillis(200))
        .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
        .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
        .take(20)
        .doOnComplete(() -> System.out.println("[doOnComplete]"));

    push.subscribe();

    Thread.sleep(5000);

    Publisher<Long> push_pull = RequestOperator.<Long>builder().source(
        Flux.interval(Duration.ofMillis(200))
            .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
            .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
            .take(20)
            .doOnComplete(() -> System.out.println("[doOnComplete]"))
    ).fetchSize(5).build();

    push_pull.subscribe(new BaseSubscriber<Long>() {});

    Thread.sleep(5000);
  }
}

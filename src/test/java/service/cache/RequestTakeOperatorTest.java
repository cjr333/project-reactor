package service.cache;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class RequestTakeOperatorTest {
  @Test
  public void test() throws InterruptedException {
    Publisher<Long> push = Flux.interval(Duration.ofMillis(200))
            .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
            .takeWhile(aLong -> aLong < 13)
            .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
            .doOnComplete(() -> System.out.println("[doOnComplete]"));

    push.subscribe(new BaseSubscriber<Long>() {});

    Thread.sleep(5000);

    Publisher<Long> push_pull = RequestTakeSingleOperator.<Long>builder().source(
        Flux.interval(Duration.ofMillis(200))
            .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
            .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
//            .take(20)
            .doOnComplete(() -> System.out.println("[doOnComplete]"))
    ).takeWhile(aLong -> aLong < 13)
        .fetchSize(5).build();

    push_pull.subscribe(new BaseSubscriber<Long>() {
      @Override
      protected void hookOnNext(Long value) {
        System.out.println("[hookOnNext] " + value);
      }
    });

    Thread.sleep(5000);
  }
}

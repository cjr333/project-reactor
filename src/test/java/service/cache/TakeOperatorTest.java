package service.cache;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class TakeOperatorTest {
  @Test
  public void test() throws InterruptedException {
    Flux<Long> push = Flux.interval(Duration.ofMillis(200))
        .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
        .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
        .take(20)
        .doOnComplete(() -> System.out.println("[doOnComplete]"));

    push.subscribe();

    Thread.sleep(5000);

    Publisher<Long> push_pull = TakeOperator.<Long>builder().source(
        Flux.interval(Duration.ofMillis(200))
            .doOnRequest(value -> System.out.println("[doOnRequest] " + value))
            .doOnNext(aLong -> System.out.println("[doOnNext] " + aLong))
            .take(20)
            .doOnComplete(() -> System.out.println("[doOnComplete]"))
    ).take(5).build();

    push_pull.subscribe(new Subscriber<Long>() {
      @Override
      public void onSubscribe(Subscription s) {

      }

      @Override
      public void onNext(Long aLong) {

      }

      @Override
      public void onError(Throwable t) {

      }

      @Override
      public void onComplete() {

      }
    });

    Thread.sleep(5000);
  }
}

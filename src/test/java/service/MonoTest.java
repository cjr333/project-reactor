package service;

import org.junit.Test;
import reactor.core.publisher.Mono;

public class MonoTest {
  @Test
  public void emtpyTest() throws InterruptedException {
    composeLog(Mono.empty())
        .subscribe(integer -> System.out.println("[Subscriber] " + integer));

    composeLog(Mono.just(1))
        .subscribe(integer -> System.out.println("[Subscriber] " + integer));
    Thread.sleep(10000);
  }

  private <T> Mono<T> composeLog(Mono<T> actual) {
    return actual.doOnNext(t -> System.out.println("[doOnNext] " + t))
        .doOnSuccess(t -> System.out.println("[doOnSuccess] " + t))
        .doOnTerminate(() -> System.out.println("[doOnTerminate]"));
  }
}

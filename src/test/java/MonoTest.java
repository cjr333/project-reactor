import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

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

  @Test
  public void retryTest() throws InterruptedException {
    System.out.println("No delay");
    Mono.error(new NullPointerException())
        .doOnError(throwable -> System.out.println(System.currentTimeMillis()))
        .retry(3)
        .subscribe();
    Thread.sleep(2000);

    System.out.println("Constant delay");
    Mono.error(new NullPointerException())
        .doOnError(throwable -> System.out.println(System.currentTimeMillis()))
        .retryBackoff(3, Duration.ofSeconds(1), Duration.ofSeconds(1))
        .subscribe(null, System.out::println);
    Thread.sleep(5000);

    System.out.println("Backoff delay");
    Mono.error(new NullPointerException())
        .doOnError(throwable -> System.out.println(System.currentTimeMillis()))
        .retryBackoff(3, Duration.ofMillis(234))
        .subscribe(null, System.out::println);
    Thread.sleep(5000);
  }

  @Test
  public void monoVoidTest() {
    StepVerifier.create(
        voidSource(false)
            .retryBackoff(3, Duration.ofMillis(100), Duration.ofMillis(100))
            .doOnError(throwable -> System.out.println("[doOnError] " + throwable.getMessage()))
    )
        .verifyError(RuntimeException.class);
  }

  private Mono<Void> voidSource(boolean success) {
    return Mono.defer(() -> success ? Mono.empty() : Mono.error(new RuntimeException("operating failed")));
  }
}

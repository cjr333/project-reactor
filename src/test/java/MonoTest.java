import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

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

  @Test
  public void mergeWithTest() {
    Mono<Void> neverMono = Mono.never();
    Flux<Void> fluxVoid = Flux.range(0, 10).flatMap(integer -> Mono.empty());

//    fluxVoid = fluxVoid.doOnComplete(() -> System.out.println("Flux completed"));
//    fluxVoid.blockLast();

    // mergeWith 는 하나의 스트림이 종료되더라도 종료되지 않는다!!!!
    // 단순 Mono<Void> Stream 의 경우는 zipWith 로 묶으면 되지만 실제 데이터가 필요한 경우는 아래 케이스에서 확인하자.
    Flux<Void> mergeWith = neverMono.mergeWith(fluxVoid);
    mergeWith = mergeWith.doOnComplete(() -> System.out.println("MergeWith completed"));
    mergeWith.blockLast();
  }

  @Test
  public void mergeWithCompleteTest() throws InterruptedException {
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    Mono<Integer> neverMono = Mono.<Integer>never().doOnTerminate(() -> subscriptionRef.get().cancel());
    Flux<Integer> integerFlux = Flux.range(0, 10).doOnComplete(() -> subscriptionRef.get().cancel());

//    Flux<Integer> mergeWith = neverMono.mergeWith(Flux.never())
//        .doOnSubscribe(subscriptionRef::set)
//        .doOnCancel(() -> System.out.println("MergeWith canceled"))
//        .doOnNext(integer -> System.out.println("data: " + integer))
//        .doOnComplete(() -> System.out.println("MergeWith completed"))
//        ;

    // 이해할 수 없는 현상;;;
    // 위에서처럼 doOnCancel 의 위치가 mergeWith 이후 최초로 등록되지 않으면 doOnCancel 의 핸들러호출이 되지 않는다.
    // 체이닝에서 doOn~ 의 서로간의 위치는 상관없는 것으로 알고 있는데 이 경우 그렇지가 않아서 헷갈림.
    Flux<Integer> mergeWith = neverMono.mergeWith(Flux.never())
        .doOnCancel(() -> System.out.println("MergeWith canceled"))
        .doOnSubscribe(subscriptionRef::set)
        .doOnNext(integer -> System.out.println("data: " + integer))
        .doOnComplete(() -> System.out.println("MergeWith completed"))
        ;
    mergeWith.subscribe();

    Thread.sleep(3000);
    subscriptionRef.get().cancel();
    Thread.sleep(3000);
  }
}

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FluxTest {
  AtomicInteger retryIndex = new AtomicInteger(0);

  @Test
  public void retryEntry() throws InterruptedException {
    Flux.interval(Duration.ofSeconds(1))
        .zipWith(Flux.range(1, 1000))
        .take(10)
        .elapsed()
        .skip(retryIndex.get())
        .flatMap(t2 -> nioCallCanError(t2.getT2().getT2(), t2.getT1()).doOnSuccess(s -> retryIndex.set(t2.getT2().getT2())))
        .retry(5)
        .subscribe(System.out::println);
    Thread.sleep(20000);
  }

  private Mono<String> nioCall(int idx, long elapsed) {
    return Mono.just("Success: id-" + idx);
  }

  private Mono<String> nioCallCanError(int idx, long elapsed) {
    if (elapsed >= 1000) {
      return Mono.just("Success: id-" + idx);
    } else {
      System.out.println("Retry: id-" + idx);
      throw new RuntimeException(elapsed + " is less than 1000");
    }
  }

  @Test
  public void onErrorTest() {
    Mono.just(1 / 0)
        .doOnNext(integer -> System.out.println("Next: " + integer))
        .doOnError(throwable -> System.out.println("Error: " + throwable.getMessage()))
        .subscribe();
  }

  @Test
  public void onErrorTestUsingTryCatch() {
    innerError()
        .doOnNext(integer -> System.out.println("Next: " + integer))
        .doOnError(throwable -> System.out.println("Error: " + throwable.getMessage()))
        .subscribe();
  }

  private Mono<Integer> innerError() {
    try {
      return Mono.just(1 / 0);
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Test
  public void onErrorTestUsingSupplier() {
    Mono.fromSupplier(() -> 1 / 0)
        .doOnNext(integer -> System.out.println("Next: " + integer))
        .doOnError(throwable -> System.out.println("Error: " + throwable.getMessage()))
        .subscribe();
  }

  @Test
  public void concatMapTest() throws InterruptedException {
    Flux.range(0, 100)
        .map(i -> System.currentTimeMillis())
        .concatMap(upstream -> Mono.just(String.format("%d:%d", upstream, System.currentTimeMillis())).delayElement(Duration.ofMillis(500)), 1)
        .take(50)
        .subscribe(System.out::println);
    Thread.sleep(30000);
  }

  @Test
  public void doOnNext() throws InterruptedException {
    Flux.interval(Duration.ofMillis(200))
        .doOnNext(aLong -> System.out.println("[doOnNext-Pre]" + aLong))
        .flatMap(aLong -> Mono.just(aLong).delayElement(Duration.ofSeconds(1)))
        .take(10)
        .doOnNext(aLong -> System.out.println("[doOnNext-Post]" + aLong))
        .subscribe(System.out::println);
    Thread.sleep(30000);
  }

  @Test
  public void delayTest() {
    Flux.range(0, 10)
        .doOnNext(integer -> System.out.println("Pre - " + integer))
        .concatMap(integer -> Mono.just(integer).delayElement(Duration.ofSeconds(1)), 1)
        .doOnNext(integer -> System.out.println("Post - " + integer))
        .blockLast();
  }

  @Test
  public void retryTest() throws InterruptedException {
    source(5)
        .retry(2).subscribe(
        integer -> System.out.println("element: " + integer),
        throwable -> System.err.println(throwable.getMessage())
    );
    Thread.sleep(3000);

    deferred(5)
        .retry(2).subscribe(
        integer -> System.out.println("element: " + integer),
        throwable -> System.err.println(throwable.getMessage())
    );
    Thread.sleep(3000);
  }

  private Flux<Integer> deferred(int count) {
    return Flux.defer(() -> source(count));
  }

  private Flux<Integer> source(int count) {
    System.out.println("called source");
    return Flux.range(0, count).flatMap(i -> i > 3 ? Flux.error(new IndexOutOfBoundsException("out of range")) : Flux.just(i));
  }

  // 여러가지로 테스트해 본 결과 doOn~ 핸들러 조합 시 doOnCancel 은 제일 먼저 나와야지 동작이 되며 나머지는 순서에 크게 상관이 없는 듯 하다.
  @Test
  public void handlerOrderTest() throws InterruptedException {
    AtomicReference<Subscription> subscriptionRef = new AtomicReference<>();
    Flux.range(0, 10).delayElements(Duration.ofMillis(500))
        .doOnComplete(() -> System.out.println("MergeWith completed"))
        .doOnCancel(() -> System.out.println("MergeWith canceled"))
        .doOnNext(integer -> System.out.println("data: " + integer))
        .doOnSubscribe(subscriptionRef::set)
        .subscribe();

    Thread.sleep(30000);
//    subscriptionRef.get().cancel();
    Thread.sleep(3000);
  }
}

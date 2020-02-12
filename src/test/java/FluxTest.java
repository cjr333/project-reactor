import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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
}

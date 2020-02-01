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

//                .zipWith(Flux.range(1, 1000))
//                .
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
}

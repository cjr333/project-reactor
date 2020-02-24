import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class StepVerifierTest {
    @Test
    public void normal() {
        StepVerifier.create(Flux.range(0, 10))
                .expectSubscription()
                .expectNext(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                .expectComplete()
                .verify();

        StepVerifier.create(Flux.range(0, 10))
                .expectSubscription()
                .consumeNextWith(System.out::println)
                .recordWith(ArrayList::new)
                .thenConsumeWhile(integer -> integer < 10)
                .consumeRecordedWith(this::verifyList)
                .expectComplete()
                .verify();
    }

    private void verifyList(Collection<Integer> integers) {
        if (!integers.stream().sorted().collect(Collectors.toList()).equals(integers)) {
            throw new RuntimeException("sort verification error");
        }
    }
}

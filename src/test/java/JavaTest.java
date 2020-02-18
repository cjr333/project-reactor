import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JavaTest {
  @Test
  public void curryingTest() {
    List<List<Integer>> data = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      data.add(IntStream.range(0, i).boxed().collect(Collectors.toList()));
    }

    int limit = 5;
    sliceTest(data, limit);

    limit = 7;
    sliceTest(data, limit);
  }

  private void sliceTest(List<List<Integer>> data, int limit) {
    BiFunction<List<Integer>, List<Integer>, List<Integer>> accumulator = sliceAccumulator(limit);

    List<Integer> result = new ArrayList<>();
    for (List<Integer> integers: data) {
      result = accumulator.apply(result, integers);
      if (stopSign(limit).test(result)) {
        break;
      }
    }
    System.out.println(result);
  }

  private BiFunction<List<Integer>, List<Integer>, List<Integer>> sliceAccumulator(Integer size) {
    return (integers, integers2) -> {
      integers.addAll(integers2);
      return integers.size() > size ? integers.subList(0, size) : integers;
    };
  }

  private Predicate<List<Integer>> stopSign(Integer size) {
    return integers -> integers.size() >= size;
  }

  @FunctionalInterface
  interface Function<One, Two, Three, Four> {
    public Four apply(One one, Two two, Three three);
  }
}

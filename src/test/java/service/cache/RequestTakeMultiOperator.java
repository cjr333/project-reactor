package service.cache;

import lombok.Builder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@Builder
public class RequestTakeMultiOperator<T, A> implements Publisher<A> {
  private final Publisher<T> source;
  @Builder.Default private final long fetchSize = 5;
  private BiFunction<A, T, A> accumulator;
  private A initValue;
  private Predicate<A> takeWhile;

  @Override
  public void subscribe(Subscriber<? super A> s) {
    source.subscribe(RequestTakeMultiInner.<T, A>builder()
        .actual(s)
        .fetchSize(fetchSize)
        .accumulator(accumulator)
        .initValue(initValue)
        .takeWhile(takeWhile)
        .build());
  }

  static final class RequestTakeMultiInner<T, A> implements Subscriber<T>, Subscription {
    private final Subscriber<? super A> actual;
    private final long fetchSize;
    private BiFunction<A, T, A> accumulator;
    private A accumulated;
    private Predicate<A> takeWhile;
    private long actualRequest;
    private final AtomicLong request;

    private Subscription current;

    @Builder
    RequestTakeMultiInner(Subscriber<? super A> actual, long fetchSize, BiFunction<A, T, A> accumulator, A initValue, Predicate<A> takeWhile) {
      this.actual = actual;
      this.fetchSize = fetchSize;
      this.accumulator = accumulator;
      this.accumulated = initValue;
      this.takeWhile = takeWhile;
      this.request = new AtomicLong(0);
    }

    @Override
    public void onSubscribe(Subscription s) {
      current = s;
      this.actual.onSubscribe(this);
    }

    @Override
    public void onNext(T t) {
      accumulated = accumulator.apply(accumulated, t);
      if (!takeWhile.test(accumulated)) {
        actual.onNext(accumulated);
        current.cancel();
        onComplete();
        return;
      }

      actualRequest--;
      if (this.request.decrementAndGet() == 0) {
        innerRequest(fetchSize);
      }
    }

    @Override
    public void onError(Throwable t) {
      actual.onError(t);
    }

    @Override
    public void onComplete() {
      actual.onNext(accumulated);
      actual.onComplete();
    }

    @Override
    public void request(long n) {
      actualRequest += n;
      innerRequest(fetchSize);
    }

    @Override
    public void cancel() {
      current.cancel();
    }

    private void innerRequest(long request) {
      if (actualRequest == 0) {
        onComplete();
      } else {
        current.request(Math.min(actualRequest, request));
        this.request.set(request);
      }
    }
  }
}

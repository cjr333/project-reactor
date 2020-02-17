package service.cache;

import lombok.Builder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Builder
public class RequestTakeSingleOperator<T> implements Publisher<T> {
  private final Publisher<T> source;
  @Builder.Default private final long fetchSize = 5;
  private Predicate<T> takeWhile;
//  private BiFunction<T, A, A> accumulator;
//  private Predicate<A> accumulatedCondition;

  @Override
  public void subscribe(Subscriber<? super T> s) {
    source.subscribe(RequestTakeSingle.<T>builder().actual(s).fetchSize(fetchSize).takeWhile(takeWhile).build());
  }

  static final class RequestTakeSingle<T> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> actual;
    private final long fetchSize;
    private Predicate<T> takeWhile;
    private long actualRequest;
    private final AtomicLong request;

    private Subscription current;

    @Builder
    RequestTakeSingle(Subscriber<? super T> actual, long fetchSize, Predicate<T> takeWhile) {
      this.actual = actual;
      this.fetchSize = fetchSize;
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
      if (!takeWhile.test(t)) {
        current.cancel();
        onComplete();
        return;
      }

      actual.onNext(t);
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

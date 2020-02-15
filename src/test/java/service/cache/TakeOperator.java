package service.cache;

import lombok.Builder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;

@Builder
public class TakeOperator<T> implements Publisher<T> {
  private final Publisher<T> source;
  @Builder.Default private final int take = 5;

  @Override
  public void subscribe(Subscriber<? super T> s) {
    source.subscribe(TakeInner.<T>builder().actual(s).take(take).build());
  }

  static final class TakeInner<T> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> actual;
    private final int take;
    private final AtomicInteger request;

    private Subscription current;

    @Builder
    TakeInner(Subscriber<? super T> actual, int take) {
      this.actual = actual;
      this.take = take;
      this.request = new AtomicInteger(0);
    }

    @Override
    public void onSubscribe(Subscription s) {
      current = s;
      request(take);
    }

    @Override
    public void onNext(T t) {
      actual.onNext(t);
      if (this.request.decrementAndGet() == 0) {
        request(take);
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
      current.request(take);
      this.request.set(take);
    }

    @Override
    public void cancel() {
      current.cancel();
    }
  }
}

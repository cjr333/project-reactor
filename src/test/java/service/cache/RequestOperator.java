package service.cache;

import lombok.Builder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

@Builder
public class RequestOperator<T> implements Publisher<T> {
  private final Publisher<T> source;
  @Builder.Default private final long fetchSize = 5;

  @Override
  public void subscribe(Subscriber<? super T> s) {
    source.subscribe(RequestInner.<T>builder().actual(s).fetchSize(fetchSize).build());
  }

  static final class RequestInner<T> implements Subscriber<T>, Subscription {
    private final Subscriber<? super T> actual;
    private long actualRequest;
    private final long fetchSize;
    private final AtomicLong request;

    private Subscription current;

    @Builder
    RequestInner(Subscriber<? super T> actual, long fetchSize) {
      this.actual = actual;
      this.fetchSize = fetchSize;
      this.request = new AtomicLong(0);
    }

    @Override
    public void onSubscribe(Subscription s) {
      current = s;
      this.actual.onSubscribe(this);
    }

    @Override
    public void onNext(T t) {
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

package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.function.Function;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class CacheMonoHelper<KEY, VALUE> {
  private final Function<KEY, Mono<VALUE>> supplier;
  private Cache<KEY, ? super Signal<? extends VALUE>> cache;
  private Cache<KEY, ? super Signal<? extends VALUE>> errorCache;

  @Builder
  public CacheMonoHelper(Function<KEY, Mono<VALUE>> supplier, Duration expire, Duration errorExpire, Integer maxEntry) {
    this.supplier = supplier;

    cache = Caffeine
        .newBuilder()
        .expireAfterWrite(defaultIfNull(expire, Duration.ofMinutes(1)))
        .maximumSize(defaultIfNull(maxEntry, 100))
        .build();

    errorCache = Caffeine
        .newBuilder()
        .expireAfterWrite(defaultIfNull(errorExpire, Duration.ofSeconds(5)))
        .maximumSize(defaultIfNull(maxEntry, 100))
        .build();
  }

  public Mono<VALUE> get(KEY key) {
    return Mono.defer(() -> {
      Object fromErrorCache = errorCache.asMap().get(key);
      if (fromErrorCache == null) {
        return CacheMono
            .lookup(cache.asMap(), key)
            .onCacheMissResume(Mono.defer(() -> supplier.apply(key)))
            .doOnError(throwable -> {
              @SuppressWarnings("unchecked")
              Signal<VALUE> errorSignal = (Signal<VALUE>) cache.asMap().get(key);
              errorCache.asMap().put(key, errorSignal);
              cache.invalidate(key);
            });
      } else if (fromErrorCache instanceof Signal) {
        try {
          @SuppressWarnings("unchecked")
          Signal<VALUE> errorSignal = (Signal<VALUE>) fromErrorCache;
          return Mono.just(errorSignal)
              .dematerialize();
        } catch (Throwable cause) {
          return Mono.error(new IllegalArgumentException("Content of cache for key " + key + " cannot be cast to Signal", cause));
        }
      } else {
        return Mono.error(new IllegalArgumentException("Content of cache for key " + key + " is not a Signal"));
      }
    });
  }

  public void clear() {
    cache.invalidateAll();
  }
}

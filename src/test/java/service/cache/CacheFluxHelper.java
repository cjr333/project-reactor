package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class CacheFluxHelper<KEY, VALUE> {
  private final Function<KEY, Flux<VALUE>> supplier;
  private final Class<VALUE> valueClass;
  private Cache<KEY, ? super List> cache;
  private Cache<KEY, ? super List> errorCache;

  @Builder
  public CacheFluxHelper(Function<KEY, Flux<VALUE>> supplier, Class<VALUE> valueClass, Duration expire, Duration errorExpire, Integer maxEntry) {
    this.supplier = supplier;
    this.valueClass = valueClass;

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

  public Flux<VALUE> get(KEY key) {
    return Flux.defer(() -> {
      Object fromErrorCache = errorCache.asMap().get(key);
      if (fromErrorCache == null) {
        return CacheFlux
            .lookup(cache.asMap(), key, valueClass)
            .onCacheMissResume(Flux.defer(() -> supplier.apply(key)))
            .doOnError(throwable -> {
              @SuppressWarnings("unchecked")
              List<Signal<VALUE>> errorSignals = (List<Signal<VALUE>>) cache.asMap().get(key);
              errorCache.asMap().put(key, errorSignals);
              cache.invalidate(key);
            });
      } else if (fromErrorCache instanceof List) {
        try {
          @SuppressWarnings("unchecked")
          List<Signal<VALUE>> errorSignals = (List<Signal<VALUE>>) fromErrorCache;
          return Flux.fromIterable(errorSignals)
              .dematerialize();
        } catch (Throwable cause) {
          return Flux.error(new IllegalArgumentException("Content of cache for key " + key + " cannot be cast to List<Signal>", cause));
        }
      } else {
        return Flux.error(new IllegalArgumentException("Content of cache for key " + key + " is not a List"));
      }
    });
  }

  public void clear() {
    cache.invalidateAll();
  }
}

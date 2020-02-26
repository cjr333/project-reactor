package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class CacheFluxHelper<KEY, VALUE> {
  private final Function<KEY, Flux<VALUE>> supplier;
  private final Class<VALUE> valueClass;
  private Cache<KEY, ? super List> cache;

  @Builder
  public CacheFluxHelper(Function<KEY, Flux<VALUE>> supplier, Class<VALUE> valueClass, Duration expire, Integer maxEntry) {
    this.supplier = supplier;
    this.valueClass = valueClass;

    cache = Caffeine
        .newBuilder()
        .expireAfterWrite(defaultIfNull(expire, Duration.ofMinutes(1)))
        .maximumSize(defaultIfNull(maxEntry, 100))
        .build();
  }

  public Flux<VALUE> get(KEY key) {
    return CacheFlux
        .lookup(cache.asMap(), key, valueClass)
        .onCacheMissResume(Flux.defer(() -> supplier.apply(key)));
  }

  public void clear() {
    cache.invalidateAll();
  }
}

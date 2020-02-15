package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class CacheFluxHelper<KEY, VALUE> {
  private final Supplier<Flux<VALUE>> supplier;
  private final Class<VALUE> valueClass;
  private Map<KEY, ? super List> cachedEntries;

  @Builder
  public CacheFluxHelper(Supplier<Flux<VALUE>> supplier, Class<VALUE> valueClass, Duration expire, Integer maxEntry) {
    this.supplier = supplier;
    this.valueClass = valueClass;

    Cache<KEY, ? super List> cache = Caffeine
        .newBuilder()
        .expireAfterWrite(defaultIfNull(expire, Duration.ofMinutes(1)))
        .maximumSize(defaultIfNull(maxEntry, 100))
        .build();
    cachedEntries = cache.asMap();
  }

  public Flux<VALUE> get(KEY key) {
    return CacheFlux
        .lookup(cachedEntries, key, valueClass)
        .onCacheMissResume(supplier);
  }
}

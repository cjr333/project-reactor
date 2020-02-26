package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class CacheMonoHelper<KEY, VALUE> {
  private final Function<KEY, Mono<VALUE>> supplier;
  private Cache<KEY, ? super Signal<? extends VALUE>> cache;

  @Builder
  public CacheMonoHelper(Function<KEY, Mono<VALUE>> supplier, Duration expire, Integer maxEntry) {
    this.supplier = supplier;

    cache = Caffeine
        .newBuilder()
        .expireAfterWrite(defaultIfNull(expire, Duration.ofMinutes(1)))
        .maximumSize(defaultIfNull(maxEntry, 100))
        .build();
  }

  public Mono<VALUE> get(KEY key) {
    return CacheMono
        .lookup(cache.asMap(), key)
        .onCacheMissResume(Mono.defer(() -> supplier.apply(key)));
  }

  public void clear() {
    cache.invalidateAll();
  }
}

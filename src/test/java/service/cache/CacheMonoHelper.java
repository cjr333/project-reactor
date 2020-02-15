package service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

public class CacheMonoHelper<KEY, VALUE> {
  private final Supplier<Mono<VALUE>> supplier;
  private Duration expire = Duration.ofMinutes(1);
  private int maxEntry = 100;
  private Map<KEY, ? super Signal<? extends VALUE>> cachedEntries;

  @Builder
  public CacheMonoHelper(Supplier<Mono<VALUE>> supplier, Duration expire, int maxEntry) {
    this.supplier = supplier;
    this.expire = expire;
    this.maxEntry = maxEntry;

    Cache<KEY, ? super Signal<? extends VALUE>> cache = Caffeine
        .newBuilder()
        .maximumSize(maxEntry)
        .expireAfterWrite(expire)
        .build();
    cachedEntries = cache.asMap();
  }
//
//
//  private CacheMonoHelper(Supplier<? extends Mono<? extends VALUE>> supplier) {
//    this.supplier = supplier;
//  }
//
//  public static <KEY, VALUE> CacheMonoHelper<KEY, VALUE> create(<? extends Mono<? extends VALUE>> supplier) {
//    return new CacheMonoHelper<>(supplier);
//  }
//
//  public CacheMonoHelper<T> entry(Duration duration) {
//    this.expireDuration = duration;
//    return this;
//  }
//
//  public CacheMonoHelper<T> expire(Duration duration) {
//    this.expireDuration = duration;
//    return this;
//  }

  public Mono<VALUE> get(KEY key) {
    return CacheMono
        .lookup(cachedEntries, key)
        .onCacheMissResume(supplier.get());
  }
  /**
   * String key = "myId";
   *     LoadingCache<String, Object> graphs = Caffeine
   *         .newBuilder()
   *         .maximumSize(10_000)
   *         .expireAfterWrite(5, TimeUnit.MINUTES)
   *         .refreshAfterWrite(1, TimeUnit.MINUTES)
   *         .build(key -> createExpensiveGraph(key));
   *
   *     Mono<Integer> cachedMyId = CacheMono
   *         .lookup(graphs.asMap(), key)
   *         .onCacheMissResume(repository.findOneById(key));
   */
  //  public Mono<T> cache()
}

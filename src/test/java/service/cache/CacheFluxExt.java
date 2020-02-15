//package service.cache;
//
//import reactor.cache.CacheFlux;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Signal;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.Function;
//
//public class CacheFluxExt<KEY, VALUE> extends CacheFlux {
//  private AtomicReference<Flux<VALUE>> supplier;
//  private boolean done = false;
//
//  public static <KEY, VALUE> FluxCacheBuilderMapMiss<VALUE> lookup(
//      Map<KEY, List<Signal<VALUE>>> cacheMap, KEY key) {
//    return otherSupplier ->
//        Flux.defer(() -> {
//          List<Signal<VALUE>> fromCache = cacheMap.get(key);
//          if (fromCache == null) {
//            return otherSupplier.get()
//                .materialize()
//                .collectList()
//                .doOnNext(signals -> cacheMap.put(key, signals))
//                .flatMapIterable(Function.identity())
//                .dematerialize();
//          }
//          else {
//            try {
//              List<Signal<VALUE>> fromCacheSignals = fromCache;
//              return Flux.fromIterable(fromCacheSignals)
//                  .dematerialize();
//            }
//            catch (Throwable cause) {
//              return Flux.error(new IllegalArgumentException("Content of cache for key " + key + " cannot be cast to List<Signal>", cause));
//            }
//          }
//        });
//  }
//}

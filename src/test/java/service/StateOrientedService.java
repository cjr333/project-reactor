package service;

import reactor.core.publisher.Mono;

import java.time.Duration;

public class StateOrientedService {
  private int key;

  public Mono<Integer> emit() {
    return Mono.just(key + 1).delayElement(Duration.ofSeconds(1)).doOnNext(nextKey -> key = nextKey);
  }
}

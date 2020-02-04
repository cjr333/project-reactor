package service;

import reactor.core.publisher.Mono;

import java.time.Duration;

public class StateOrientedService {
  private int key;

  public Mono<Integer> emit() {
    System.out.println("Emit: " + key);
    if (key > 5) {
      return Mono.never();
    } else {
      return Mono.just(key + 1).doOnNext(nextKey -> {
        key = nextKey;
      });
    }
  }
}

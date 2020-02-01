package service;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

import java.util.concurrent.atomic.AtomicInteger;

public class MyServiceTest {
  MyService myService = new MyService();

  @Test
  public void unlimitTest() throws InterruptedException {
    AtomicInteger subscribed = new AtomicInteger();
    int itemCount = 10;
    myService.unlimit()
        .subscribe(new BaseSubscriber<Chunk>() {
          @Override
          public void hookOnSubscribe(Subscription subscription) {
            subscription.request(1);
          }

          @Override
          public void hookOnNext(Chunk chunk) {
            if (subscribed.addAndGet(chunk.getCount()) >= itemCount) {
              cancel();
            } else {
              request(1);
            }
          }
        });
    Thread.sleep(20000);
  }

  @Test
  public void limitTest() throws InterruptedException {
    myService.limit(10).subscribe();
    Thread.sleep(20000);
  }

  @Test
  public void limitUsingUnlimitTest() throws InterruptedException {
    myService.limitUsingUnlimit(10).subscribe();
    Thread.sleep(20000);
  }

  /**
   * 조합기는 take 와 무관하게 동작하기 때문에 조합하다가 stack overflow 가 발생한다.
   */
  //  @Test
//  public void recursiveTest() throws InterruptedException {
//    myService.recursive(1)
//        .take(10)
//        .subscribe(System.out::println);
//    Thread.sleep(20000);
//  }

  @Test
  public void generateTest() throws InterruptedException {
    myService.generate(1)
        .take(10)
        .subscribe(System.out::println);
    myService.generate(10)
                .take(10)
        .subscribe(System.out::println);
    Thread.sleep(20000);
  }

  @Test
  public void concatMapTest() throws InterruptedException {
    myService.concatMap()
        .take(10)
        .subscribe(System.out::println);
    myService.concatMap()
        .take(10)
        .subscribe(System.out::println);
    Thread.sleep(20000);
  }
}

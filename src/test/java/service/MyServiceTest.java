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
                    public void hookOnNext(Chunk chunk) {
                        if (subscribed.addAndGet(chunk.getCount()) >= itemCount) {
                            cancel();
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
}

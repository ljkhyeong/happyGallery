package com.personal.happygallery.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/** 동시성 테스트의 작업을 모두 준비시킨 뒤 한 번에 시작하고 작업별 실패를 모은다. */
public final class ConcurrentExecution {

    private ConcurrentExecution() {}

    /** 반환값 없이 실행해도 되는 동시 실행 작업. */
    @FunctionalInterface
    public interface Task {
        void run() throws Exception;
    }

    /**
     * 모든 작업이 준비되면 동시에 시작하고 끝날 때까지 기다린다.
     *
     * @return 작업 순서대로 던진 예외. 성공한 작업 자리는 null이다.
     */
    public static List<Throwable> runSimultaneously(List<Task> tasks) throws Exception {
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<Throwable>> futures = tasks.stream()
                    .map(task -> executor.submit(() -> runAfterStart(task, ready, start)))
                    .toList();
            assertThat(ready.await(10, SECONDS)).as("동시 실행 준비").isTrue();
            start.countDown();
            List<Throwable> failures = new ArrayList<>(futures.size());
            for (Future<Throwable> future : futures) {
                failures.add(future.get(15, SECONDS));
            }
            return failures;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Throwable runAfterStart(Task task, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, SECONDS)) {
                return new AssertionError("동시 실행 시작 신호를 기다리지 못했습니다.");
            }
            task.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }
}

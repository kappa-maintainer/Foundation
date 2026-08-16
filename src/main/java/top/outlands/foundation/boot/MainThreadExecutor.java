package top.outlands.foundation.boot;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Executes tasks on the main thread of the process, which is also the client thread
 * that runs the game loop and owns the OpenGL context.
 */
public final class MainThreadExecutor {

    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile Thread mainThread;

    private MainThreadExecutor() {
    }

    /**
     * Captures the current thread as the main thread. Called by {@link Foundation#main}
     * on the process main thread, which is the thread the game loop ("Client thread")
     * will run on.
     */
    public static void init() {
        mainThread = Thread.currentThread();
    }

    /**
     * @return whether the current thread is the main (client) thread
     */
    public static boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    /**
     * Enqueues a task to be run on the main thread by the next {@link #drain()}.
     * Never blocks; safe to call from any thread, including while the game loop is
     * blocked.
     */
    public static void post(Runnable task) {
        QUEUE.add(task);
    }

    /**
     * Runs the task on the main thread. On the main thread this executes inline;
     * from any other thread it enqueues and blocks until the next {@link #drain()}.
     * Non-main threads should prefer {@link #post(Runnable)} to avoid blocking.
     */
    public static <T> T submit(Callable<T> task) {
        if (isMainThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        QUEUE.add(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.join();
    }

    /**
     * Runs all queued tasks on the main thread. Must be called from the main thread
     * only. A single failing task is logged and does not prevent the remaining tasks
     * from running.
     */
    public static void drain() {
        if (!isMainThread()) {
            throw new IllegalStateException("drain() must be called on the main thread");
        }
        Runnable task;
        while ((task = QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Throwable t) {
                Foundation.LOGGER.error("Task on the main thread executor failed", t);
            }
        }
    }
}

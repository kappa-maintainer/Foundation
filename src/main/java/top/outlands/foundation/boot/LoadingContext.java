package top.outlands.foundation.boot;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.jar.Manifest;

/**
 * Per-thread stack of the loading context (package, manifest, code source) of the
 * class currently being loaded by {@link ActualClassLoader}. {@link
 * ActualClassLoader#findClass(String)} pushes the context before running the
 * transformer chain and pops it afterwards, so transformers can inspect the class
 * being loaded through {@link #current()} without any signature change to the
 * transformer interfaces.
 *
 * <p>Nested loads on the same thread (a transformer loading another class) push
 * their own context, and concurrent loads on different threads are isolated by the
 * thread local. No context is retained once the load finishes.
 */
public record LoadingContext(Package pkg, Manifest manifest, URL codeSourceUrl) {

    private static final ThreadLocal<Deque<LoadingContext>> STACK = new ThreadLocal<>();

    private static Deque<LoadingContext> stack() {
        Deque<LoadingContext> stack = STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            STACK.set(stack);
        }
        return stack;
    }

    /**
     * Pushes a loading context for the current thread. Must be paired with {@link #pop()}.
     */
    public static void push(Package pkg, Manifest manifest, URL codeSourceUrl) {
        stack().push(new LoadingContext(pkg, manifest, codeSourceUrl));
    }

    /**
     * Pops the innermost loading context of the current thread.
     */
    public static void pop() {
        Deque<LoadingContext> stack = STACK.get();
        if (stack == null || stack.isEmpty()) {
            return; // defensive: an unbalanced pop must never break class loading
        }
        stack.pop();
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * @return the loading context of the innermost class load on the current thread,
     *         or {@code null} if no class is currently being loaded by the actual class loader
     */
    public static LoadingContext current() {
        Deque<LoadingContext> stack = STACK.get();
        return stack == null ? null : stack.peek();
    }
}

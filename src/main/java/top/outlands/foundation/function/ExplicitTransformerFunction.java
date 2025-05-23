package top.outlands.foundation.function;

@FunctionalInterface
public interface ExplicitTransformerFunction<T> {
    byte[] apply(String name, byte[] basicClass);
}

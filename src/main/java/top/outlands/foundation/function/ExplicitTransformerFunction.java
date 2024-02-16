package top.outlands.foundation.function;

@FunctionalInterface
public interface ExplicitTransformerFunction {
    byte[] apply(String name, byte[] basicClass);
}

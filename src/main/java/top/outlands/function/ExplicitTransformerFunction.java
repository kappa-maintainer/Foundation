package top.outlands.function;

@FunctionalInterface
public interface ExplicitTransformerFunction {
    byte[] apply(String name, byte[] basicClass);
}

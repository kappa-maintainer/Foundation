package top.outlands.function;

@FunctionalInterface
public interface TransformerFunction {
    byte[] apply(final String name, final String transformedName, byte[] basicClass);
}

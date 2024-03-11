package top.outlands.foundation.function;

import java.util.jar.Manifest;

@FunctionalInterface
public interface TransformerFunction {
    byte[] apply(final String name, final String transformedName, byte[] basicClass);
}

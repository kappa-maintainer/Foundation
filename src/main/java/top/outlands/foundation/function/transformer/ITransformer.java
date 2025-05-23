package top.outlands.foundation.function.transformer;

/**
 * Only for global transforming; if you know what your targets are, use {@link top.outlands.foundation.function.transformer.IExplicitTransformer}.
 */
@FunctionalInterface
public interface ITransformer<T> extends IHasPriority {
    T transform(String name, String transformedName, T basicClass);
}
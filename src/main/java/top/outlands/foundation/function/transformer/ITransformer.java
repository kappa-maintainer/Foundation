package top.outlands.foundation.function.transformer;

/**
 * Only for global transforming; if you know what your targets are, use {@link top.outlands.foundation.function.transformer.IExplicitTransformer}.
 */
@FunctionalInterface
public interface ITransformer<T> extends IHasPriority {
    /**
     * @param name Untransformed class name. Not sure why it exists. Do not use.
     * @param transformedName Transformed class name.
     * @param basicClass Class.
     * @return Transformed class.
     */
    T transform(String name, String transformedName, T basicClass);
}
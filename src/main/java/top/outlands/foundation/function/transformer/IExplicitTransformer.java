package top.outlands.foundation.function.transformer;

/**
 * The new transformer type. It can bypass transformer exclusion but can't do wildcards matching.
 */
@FunctionalInterface
public interface IExplicitTransformer<T> extends IHasPriority {
 
     * @param basicClass Class. Only classes matching transformed name will be fed.
     * @return Modified Class.
     */
    T transform(T basicClass);
}
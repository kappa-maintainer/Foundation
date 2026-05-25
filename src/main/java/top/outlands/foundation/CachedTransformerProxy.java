package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Wraps an {@link IClassTransformer} with a per-class output cache lookup
 * (see {@link TransformerCache}). On hit, returns cached output without
 * invoking the wrapped transformer.
 */
public class CachedTransformerProxy implements IClassTransformer {

    protected final IClassTransformer wrapped;
    protected final String wrappedName;

    public CachedTransformerProxy(IClassTransformer wrapped) {
        this.wrapped = wrapped;
        this.wrappedName = wrapped.getClass().getName();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            // Some transformers tolerate null; pass through.
            return wrapped.transform(name, transformedName, basicClass);
        }
        byte[] cached = TransformerCache.get(wrappedName, transformedName, basicClass);
        if (cached != null) {
            // "no-change" output is represented as an empty marker; in that case return input.
            return cached.length == 0 ? basicClass : cached;
        }
        byte[] result = wrapped.transform(name, transformedName, basicClass);
        TransformerCache.put(wrappedName, transformedName, basicClass, result);
        return result;
    }

    /** Underlying wrapped transformer (for tooling/introspection). */
    public IClassTransformer getWrapped() {
        return wrapped;
    }

    /** Class name of the underlying wrapped transformer. */
    public String delegateClassName() {
        return wrappedName;
    }
}

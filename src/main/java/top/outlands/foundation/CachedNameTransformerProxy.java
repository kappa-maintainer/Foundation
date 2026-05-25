package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Variant of {@link CachedTransformerProxy} that also implements
 * {@link IClassNameTransformer} (used by FML's deobf transformer's
 * sibling roles). Forge code checks {@code instanceof IClassNameTransformer}
 * on the registered transformer; the plain proxy would hide the wrapped
 * instance from those checks, so we expose the interface here.
 */
public class CachedNameTransformerProxy extends CachedTransformerProxy implements IClassNameTransformer {

    private final IClassNameTransformer renameDelegate;

    public <T extends IClassNameTransformer & IClassTransformer> CachedNameTransformerProxy(T wrapped) {
        super(wrapped);
        this.renameDelegate = wrapped;
    }

    @Override
    public String remapClassName(String name) {
        return renameDelegate.remapClassName(name);
    }

    @Override
    public String unmapClassName(String name) {
        return renameDelegate.unmapClassName(name);
    }
}

package top.outlands.foundation;

/**
 * The new transformer type. It can bypass transformer exclusion but can't do wildcards matching.
 */
public interface IExplicitTransformer {
    /**
     * Similar to {@link net.minecraft.launchwrapper.IClassTransformer#transform(String, String, byte[])}
     * @param transformedName The transformed name.
     * @param basicClass Class bytes.
     * @return Modified class bytes.
     */
    byte[] transform(String transformedName, byte[] basicClass);

    /**
     * Override this to set your transformer's priority.
     * @return The priority int. 
     */
    default int getPriority() {
        return 0;
    }
}

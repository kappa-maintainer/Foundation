package top.outlands.foundation;

/**
 * The new transformer type. It can bypass transformer exclusion but can't do wildcards matching.
 */
public interface IExplicitTransformer {
    /**
     * Similar to {@link net.minecraft.launchwrapper.IClassTransformer#transform(String, String, byte[])}
     * @param basicClass Class bytes. Only classes matching transformed name will be fed.
     * @return Modified class bytes.
     */
    byte[] transform(byte[] basicClass);

    /**
     * Override this to set your transformer's priority.
     * @return The priority int. 
     */
    default int getPriority() {
        return 0;
    }
}

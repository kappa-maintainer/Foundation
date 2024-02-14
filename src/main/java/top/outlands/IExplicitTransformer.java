package top.outlands;

/**
 * The new transformer type. It can bypass transformer exclusion but can't do wildcards matching.
 */
public interface IExplicitTransformer extends Comparable<IExplicitTransformer> {
    /**
     * Similar to {@link net.minecraft.launchwrapper.IClassTransformer#transform}
     * @param transformedName The transformed name.
     * @param basicClass Class bytes.
     * @return Modified class bytes.
     */
    byte[] transform(String transformedName, byte[] basicClass);

    /**
     * By default, it will compare by class name. 
     * @param another Another transformer
     * @return 0, 1 or -1
     */
    @Override
    default int compareTo(IExplicitTransformer another) {
        return Integer.compare(this.getPriority(), another.getPriority());
    }

    /**
     * Override this to set your transformer's priority.
     * @return The priority int. 
     */
    default int getPriority() {
        return 0;
    }
}

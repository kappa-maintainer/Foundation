package net.minecraft.launchwrapper;

import java.util.jar.Manifest;

/**
 * Good old transformer interface. Only use this for global transforming; if you know what your targets are, use {@link top.outlands.foundation.IExplicitTransformer}.
 */
public interface IClassTransformer {
    /**
     * Original transform method, could be replaced by the new one with manifest. (In case you want to check it)
     * @param name Untransformed class name. Not sure why it exists. Do not use.
     * @param transformedName Transformed class name.
     * @param basicClass Class bytes.
     * @return Transformed class bytes.
     */
    byte[] transform(String name, String transformedName, byte[] basicClass);

    /**
     * Override this to set your transformer's priority.
     * @return The priority int.
     */
    default int getPriority() {
        return 0;
    }
}

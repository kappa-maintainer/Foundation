package net.minecraft.launchwrapper;

import java.util.jar.Manifest;

/**
 * Good old transformer interface. Only use this for global transforming; if you know what your targets are, use {@link top.outlands.foundation.IExplicitTransformer}.
 */
public interface IClassTransformer {
    /**
     * Original transform method, could be replaced by the new one with manifest. (You still need to implement an empty method of this)
     * @param name Raw class name. Not sure why it exists. Do not use.
     * @param remappedName Remapped class name.
     * @param bytes Class bytes.
     * @return Transformed class bytes.
     */
    byte[] transform(String name, String remappedName, byte[] bytes);

    /**
     * New transform method actually been called.
     * @param name Raw class name. Not sure why it exists. Do not use.
     * @param remappedName Remapped class name.
     * @param bytes Class bytes.
     * @param pkg The {@link Package} instance of loading class. Used for annotation checking. 
     * @param manifest The {@link Manifest} instance of loading class. Used for manifest checking.
     * @return Transformed class bytes.
     */
    default byte[] transform(String name, String remappedName, byte[] bytes, Package pkg, Manifest manifest) {
        return transform(name, remappedName, bytes);
    }

    /**
     * Override this to set your transformer's priority.
     * @return The priority int.
     */
    default int getPriority() {
        return 0;
    }
}

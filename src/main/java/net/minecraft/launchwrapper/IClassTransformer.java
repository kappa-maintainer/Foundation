package net.minecraft.launchwrapper;

import java.util.jar.Manifest;

public interface IClassTransformer {
    /**
     * Original transform method, could be replaced by the new one with manifest.
     * @param name Untransformed class name. Not sure why it exists. Do not use.
     * @param transformedName Transformed class name.
     * @param basicClass Class bytes.
     * @return Transformed class bytes.
     */
    byte[] transform(String name, String transformedName, byte[] basicClass);

    /**
     * The new transform method with manifest. Transformers could use this data to determine transformation action.
     * @param name Untransformed class name. Kept for compatibility. Do not use.
     * @param transformedName Transformed class name.
     * @param basicClass Class bytes.
     * @param manifest Jar manifest.
     * @return Transformed class bytes.
     */
    default byte[] transform(String name, String transformedName, byte[] basicClass, Manifest manifest) {
        return transform(name, transformedName, basicClass);
    }
}

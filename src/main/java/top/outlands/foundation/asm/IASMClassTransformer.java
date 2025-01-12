package top.outlands.foundation.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import top.outlands.foundation.IExplicitTransformer;

/**
 * Packaged as ASM to make it more convenient.
 */
@FunctionalInterface
public interface IASMClassTransformer extends IExplicitTransformer{
    /**
     * @param classNode node
     * @return the flags, {@link ClassWriter}
     */
    int transform(ClassNode classNode);

    @Override
    @Deprecated
    default byte[] transform(byte[] bytes){
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        ClassWriter classWriter = new ClassWriter(this.transform(classNode));
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}

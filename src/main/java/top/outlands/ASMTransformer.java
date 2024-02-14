package top.outlands;

import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;

public class ASMTransformer implements IExplicitTransformer{
    @Override
    public byte[] transform(String transformedName, byte[] basicClass) {
        try {
            CtClass cc = ClassPool.getDefault().makeClass(new ByteArrayInputStream(basicClass));
            var cotr = cc.getConstructor("(I)V");
            cotr.insertBefore(
                    "if (api != 589824) {api = 589824;}"
            );
            cotr = switch (cc.getSimpleName()) {
                case "FieldVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/FieldVisitor;)V");
                case "ClassVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/ClassVisitor;)V");
                case "MethodVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/MethodVisitor;)V");
                default -> throw new IllegalStateException("Unexpected value: " + cc.getSimpleName());
            };
            cotr.insertBefore(
                    "if (api != 589824) {api = 589824;}"
            );
            basicClass = cc.toBytecode();
        }catch (Throwable t) {
            Foundation.LOGGER.error(t);
        }
        return basicClass;
    }
}

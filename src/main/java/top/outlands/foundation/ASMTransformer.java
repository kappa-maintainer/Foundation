package top.outlands.foundation;

import javassist.ClassPool;
import javassist.CtClass;
import top.outlands.foundation.boot.Foundation;

import java.io.ByteArrayInputStream;

import static top.outlands.foundation.boot.Foundation.LOGGER;

public class ASMTransformer implements IExplicitTransformer{
    private static final String CODE = 
                    """
                        if (api < 589824) {
                            this.api = 589824;
                            top.outlands.foundation.boot.Foundation.OUTDATED_VISITOR.add(this.getClass().getName());
                        }
                    """;
    @Override
    public byte[] transform(String transformedName, byte[] basicClass) {
        LOGGER.info("Patching " + transformedName);
        try {
            CtClass cc = ClassPool.getDefault().makeClass(new ByteArrayInputStream(basicClass));
            var cotr = cc.getConstructor("(I)V");
            cotr.insertAfter(CODE);
            cotr = switch (cc.getSimpleName()) {
                case "FieldVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/FieldVisitor;)V");
                case "ClassVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/ClassVisitor;)V");
                case "MethodVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/MethodVisitor;)V");
                default -> throw new IllegalStateException("Unexpected value: " + cc.getSimpleName());
            };
            cotr.insertAfter(CODE);
            //cc.debugWriteFile("./dump");
            basicClass = cc.toBytecode();
        }catch (Throwable t) {
            LOGGER.error(t);
        }
        return basicClass;
    }
}

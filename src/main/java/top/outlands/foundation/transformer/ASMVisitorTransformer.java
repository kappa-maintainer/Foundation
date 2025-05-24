package top.outlands.foundation.transformer;

import javassist.CtClass;
import top.outlands.foundation.function.transformer.IExplicitTransformer;

import java.io.ByteArrayInputStream;

import static top.outlands.foundation.boot.Foundation.LOGGER;

public class ASMVisitorTransformer implements IExplicitTransformer<CtClass> {
    private static final String CODE = 
                    """
                        if (api < 589824) {
                            this.api = 589824;
                            top.outlands.foundation.boot.Foundation.add(this.getClass().getName());
                        }
                    """;
    @Override
    public byte[] transform(byte[] cc) {
        LOGGER.debug("Patching " + cc.getName());
        var cotr = cc.getConstructor("(I)V");
        cotr.insertAfter(CODE);
        cotr = switch (cc.getSimpleName()) {
            case "FieldVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/FieldVisitor;)V");
            case "ClassVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/ClassVisitor;)V");
            case "MethodVisitor" -> cc.getConstructor("(ILorg/objectweb/asm/MethodVisitor;)V");
            default -> throw new IllegalStateException("Unexpected value: " + cc.getSimpleName());
        };
        cotr.insertAfter(CODE);
        return cc;
    }
}

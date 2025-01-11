package top.outlands.foundation.transformer;

import javassist.ClassPool;
import javassist.CtClass;
import net.minecraft.launchwrapper.Launch;
import top.outlands.foundation.IExplicitTransformer;

import java.io.ByteArrayInputStream;

import static top.outlands.foundation.boot.Foundation.LOGGER;

public class ASMClassWriterTransformer implements IExplicitTransformer {
    @Override
    public byte[] transform(byte[] basicClass) {
        try {
            var cp = ClassPool.getDefault();
            CtClass cc = cp.makeClass(new ByteArrayInputStream(basicClass));
            LOGGER.debug("Patching " + cc.getName());
            var method = cc.getDeclaredMethod("getCommonSuperClass");
            method.setBody("{return top.outlands.foundation.transformer.ASMClassWriterTransformer.getCommonSuperClass($$);}");

            //cc.debugWriteFile("./dump");
            basicClass = cc.toBytecode();
        }catch (Throwable t) {
            LOGGER.error(t);
        }
        return basicClass;
    }
    public static String getCommonSuperClass(final String type1, final String type2) {
        return top.outlands.foundation.asm.LaunchClassWriter.getCommonSuperClass0(type1, type2);
    }

}

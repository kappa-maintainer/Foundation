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
        Class<?> class1;
        Class<?> class2;

        ClassLoader classLoader = Launch.appClassLoader;
        try { // Make sure classes in same class loader
            class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
            class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (ClassNotFoundException e) {
            try {
                class1 = Class.forName(type1.replace('/', '.'), false, Launch.classLoader);
                class2 = Class.forName(type2.replace('/', '.'), false, Launch.classLoader);
            } catch (ClassNotFoundException e) {
                throw new TypeNotPresentException(type2, e);
            }
        }

        if (class1.isAssignableFrom(class2)) {
            return type1;
        }
        if (class2.isAssignableFrom(class1)) {
            return type2;
        }
        if (class1.isInterface() || class2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getName().replace('.', '/');
        }
    }

}

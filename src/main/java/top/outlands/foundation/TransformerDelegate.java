package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.boot.TransformerHolder;
import top.outlands.foundation.util.ExplicitTransformerList;
import top.outlands.foundation.util.TransformerList;
import top.outlands.foundation.function.transformer.ITransformer;
import top.outlands.foundation.transformer.ASMClassWriterTransformer；

import javassist.ClassPool;
import javassist.CtClass;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.io.ByteArrayInputStream;

import static net.minecraft.launchwrapper.Launch.classLoader;
import static top.outlands.foundation.boot.Foundation.LOGGER;
import static top.outlands.foundation.boot.TransformerHolder.*;

/**
 * A delegate to new and old transformer-related methods.
 */
public class TransformerDelegate {

    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("foundation.verbose", "false"));
    
    /**
     * @return list of transformers.
     */
    public static List<IClassTransformer> getTransformers() {
        return transformers;
    }

    /**
     * Get explicit transformers map. It's exact same map in used, you can modify it at will.
     * @return the map
     */
    @Deprecated
    public static Map<String, PriorityQueue<IExplicitTransformer>> getExplicitTransformers() {
        return explicitClassByteTransformers.getTransformers();
    }

    /**
     * Checking this every registration is dumb, so let's make another method
     * Do not use if you aren't Forge
     * @param transformer The name transformer instance
     */
    public static void registerRenameTransformer(IClassNameTransformer transformer) {
        LOGGER.debug("Registering rename transformer: {}", transformer.getClass().getSimpleName());
        if (renameTransformer == null) {
            renameTransformer = transformer;
            registerTransformer((IClassTransformer) transformer);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param targets Target classes' name.
     * @param className Class name of the transformer.
     */
    @Deprecated
    public static void registerExplicitTransformer(String className, String... targets) {
        if (targets.length == 0) return;
        LOGGER.debug("Registering explicit transformer: {}", className);
        if (!className.contains(".")) {
            className = className.replace('/', '.');
        }
        try {
            IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(className).getConstructor().newInstance();
            registerExplicitTransformer(instance, targets);
        } catch (Exception e) {
            LOGGER.error("Error registering explicit transformer class {}", className, e);
        }
    }

    @Deprecated
    public static void registerExplicitTransformer(IExplicitTransformer transformer, String... targets) {
        explicitClassByteTransformers.register(transformer, targets);
    }

    public static void registerExplicitClassByteTransformer(top.outlands.foundation.function.transformer.IExplicitTransformer<byte[]> transformer, String... targets) {
        explicitClassByteTransformers.register(transformer, targets);
    }

    public static void registerExplicitClassNodeTransformer(top.outlands.foundation.function.transformer.IExplicitTransformer<ClassNode> transformer, String... targets) {
        explicitClassNodeTransformers.register(transformer, targets);
    }

    public static void registerExplicitClassNodeTransformer(top.outlands.foundation.function.transformer.IExplicitTransformer<ClassVisitor> transformer, String... targets) {
        explicitClassVisitorTransformers.register(transformer, targets);
    }

    public static void registerExplicitClassNodeTransformer(top.outlands.foundation.function.transformer.IExplicitTransformer<CtClass> transformer, String... targets) {
        explicitCtClassTransformers.register(transformer, targets);
    }

    /**
     * Same as {@link net.minecraft.launchwrapper.LaunchClassLoader#registerTransformer(String)}
     * @param transformerClassName class name
     */
    public static void registerTransformer(String transformerClassName) {
        if (!transformerClassName.contains(".")) {
            transformerClassName = transformerClassName.replace('/', '.');
        }
        LOGGER.debug("Registering transformer: {}", transformerClassName);
        try {
            IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(transformerClassName).getConstructor().newInstance();
            transformers.add(transformer);
            transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
        } catch (Exception e) {
            LOGGER.error("Error registering transformer class {}", transformerClassName, e);
        }
    }

    /**
     * In case you want to control how the transformer is initialized, in which you could <b>new</b> it yourself.
     * @param transformer The transformer
     */
    @Deprecated
    public static void registerTransformer(IClassTransformer transformer) {
        LOGGER.debug("Registering transformer instance: {}", transformer.getClass().getName());
        transformers.add(transformer);
    }

    public static void registerClassByteTransformer(ITransformer<byte[]> transformer) {
        classByteTransformers.register(transformer);
    }

    public static void registerClassNodeTransformer(ITransformer<ClassNode> transformer) {
        classNodeTransformers.register(transformer);
    }

    public static void registerClassVisitorTransformer(ITransformer<ClassVisitor> transformer) {
        classVisitorTransformers.register(transformer);
    }

    public static void registerCtClassTransformer(ITransformer<CtClass> transformer) {
        ctClassTransformers.register(transformer);
    }

    /**
     * Call this with class name to remove all transformers with target class name.
     * @param name The transformer name you want to un-register
     */
    public static void unRegisterTransformer(final String name) {
        LOGGER.debug("Unregistering all transformers call: {}", name);
        classByteTransformers.unregister(transformer -> name.equals(transformer.getClass().getName()));
    }

    /**
     * Call this to remove your transformer, you need to keep track of the instances yourself.
     * @param transformer The transformer you want to un-register
     */
    public static void unRegisterTransformer(IClassTransformer transformer) {
        classByteTransformers.unregister(transformer);
    }

    /**
     * We use lambda trick to fill method implementations after the class loader ready
     * @param holder The one and only handler
     */
    static void fillTransformerHolder(TransformerHolder holder) {
        explicitClassByteTransformers = new ExplicitTransformerList<>(new HashMap<>(20), Foundation.LOGGER);
        explicitClassNodeTransformers = new ExplicitTransformerList<>(new HashMap<>(20), Foundation.LOGGER);
        explicitClassVisitorTransformers = new ExplicitTransformerList<>(new HashMap<>(20), Foundation.LOGGER);
        explicitCtClassTransformers = new ExplicitTransformerList<>(new HashMap<>(20), Foundation.LOGGER);

        classByteTransformers = new TransformerList<>((List<ITransformer<byte>>)(Object)(transformers = new LinkedList<>()), Foundation.LOGGER);
        classNodeTransformers = new TransformerList<>(new LinkedList(), Foundation.LOGGER);
        classVisitorTransformers = new TransformerList<>(new LinkedList(), Foundation.LOGGER);
        ctClassTransformers = new TransformerList<>(new LinkedList(), Foundation.LOGGER);

        holder.runTransformersFunction = classByteTransformers::run;
        holder.registerTransformerFunction = s -> {
            if (!s.contains(".")) {
                s = s.replace('/', '.');
            }
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).getConstructor().newInstance();
                classByteTransformers.register(transformer);
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        holder.runExplicitTransformersFunction = explicitClassByteTransformers::run;
        holder.runASMTransformersFunction = (name, transformedName, basicClass) -> {
            if (basicClass == null) return null;
            ClassReader classReader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            classReader.accept(explicitClassVisitorTransformers.run(transformedName, classVisitorTransformers.run(name, transformedName, classNode)));
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS){
                @Override
                public String getCommonSuperClass(String s1, String s2) {
                    return ASMClassWriterTransformer.getCommonSuperClass(s1, s2);
                }
            };
            explicitClassNodeTransformers.run(transformedName, classNodeTransformers.run(name, transformedName, classNode)).accept(classWriter);
            return classWriter.toByteArray();
        }
        holder.runJavassistTransformersFunction = (name, transformedName, basicClass) -> {
            if (basicClass == null) return null;
            var cp = ClassPool.getDefault();
            CtClass cc = cp.makeClass(new ByteArrayInputStream(basicClass));
            return explicitCtClassTransformers.run(transformedName, ctClassTransformers.run(name, transformedName, cc)).toBytecode();
        }
        holder.transformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.remapClassName(s);
            }
            return s;
        };
        holder.unTransformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.unmapClassName(s);
            }
            return s;
        };
        holder.debugPrinter = () -> {
            LOGGER.info("Running transformers: ");
            getTransformers().stream().map(t -> t.toString() + " : " + t.getPriority()).forEach(s -> LOGGER.info(s));
            LOGGER.info("Transformer Exclusions: ");
            classLoader.getTransformerExclusions().forEach(s -> LOGGER.info(s));
            LOGGER.info("Class Paths: ");
            Arrays.stream(classLoader.getURLs()).forEach(s -> LOGGER.info(s));
        };
    }
}

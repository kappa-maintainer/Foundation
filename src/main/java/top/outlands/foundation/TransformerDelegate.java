package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.boot.TransformerHolder;

import java.util.*;

import static net.minecraft.launchwrapper.Launch.classLoader;
import static top.outlands.foundation.boot.Foundation.LOGGER;
import static top.outlands.foundation.boot.TransformerHolder.*;

/**
 * A delegate to new and old transformer-related methods.
 */
public class TransformerDelegate {

    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("foundation.verbose", "false"));
    /**
     * The original getTransformers() was in {@link net.minecraft.launchwrapper.LaunchClassLoader}, but that may cause unwanted classloading.
     * The list itself is a skip list set now, so you can't change it by modifying the return value here.
     * @return list of transformers.
     */
    public static List<IClassTransformer> getTransformers() {
        return transformers;
    }

    /**
     * Get explicit transformers map. It's exact same map in used, you can modify it at will.
     * @return the map
     */
    public static Map<String, PriorityQueue<IExplicitTransformer>> getExplicitTransformers() {
        return explicitTransformers;
    }

    /**
     * Checking this every registration is dumb, so let's make another method
     * Do not use if you aren't Forge
     * @param transformer The name transformer instance
     */
    public static void registerRenameTransformer(IClassNameTransformer transformer) {
        LOGGER.debug("Registering rename transformer: " + transformer.getClass().getSimpleName());
        if (renameTransformer == null) {
            renameTransformer = transformer;
            registerTransformer((IClassTransformer) transformer);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param targets Target classes' name. Transformed Name.
     * @param className Class name of the transformer.
     */
    public static void registerExplicitTransformer(String className, String... targets) {
        if (targets.length == 0) return;
        if (className.indexOf('/') != -1) {
            className = className.replace('/', '.');
        }
        try {
            IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(className).getConstructor().newInstance();
            registerExplicitTransformer(instance, targets);
        } catch (Exception e) {
            LOGGER.error("Error registering explicit transformer class {}", className, e);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param targets Target classes' name. Transformed Name.
     * @param transformer transformer
     */
    public static void registerExplicitTransformer(IExplicitTransformer transformer, String... targets) {
        LOGGER.debug("Registering explicit transformer: " + transformer.getClass().getSimpleName());
        if (targets.length == 0) return;
        try {
            for (var target : targets) {
                if (explicitTransformers.containsKey(target)) {
                    explicitTransformers.get(target).add(transformer);
                } else {
                    var transformerSet = new PriorityQueue<>(Comparator.comparingInt(IExplicitTransformer::getPriority));
                    transformerSet.add(transformer);
                    explicitTransformers.put(target, transformerSet);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error registering explicit transformer class {}", transformer.getClass().getSimpleName(), e);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param className Class name of the transformer.
     */
    public static void registerExplicitTransformer(String className) {
        if (className.indexOf('/') != -1) {
            className = className.replace('/', '.');
        }
        try {
            if (Launch.classLoader.loadClass(className).getConstructor().newInstance() instanceof ITargetedTransformer targeted) {
                TransformerDelegate.registerExplicitTransformer(targeted.getTransformer(), targeted.getTargets());
            }
        } catch (Exception e) {
            Foundation.LOGGER.error("Error registering explicit transformer class {}", className, e);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param transformer transformer
     */
    public static void registerExplicitTransformer(ITargetedTransformer transformer) {
        TransformerDelegate.registerExplicitTransformer(transformer.getTransformer(), transformer.getTargets());
    }

    /**
     * Same as {@link net.minecraft.launchwrapper.LaunchClassLoader#registerTransformer(String)}
     * @param transformerClassName class name
     */
    public static void registerTransformer(String transformerClassName) {
        if (!transformerClassName.contains(".")) {
            transformerClassName = transformerClassName.replace('/', '.');
        }
        LOGGER.debug("Registering transformer: " + transformerClassName);
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
    public static void registerTransformer(IClassTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * Call this with class name to remove all transformers with target class name.
     * @param name The transformer name you want to un-register
     */
    public static void unRegisterTransformer(String name) {
        LOGGER.debug("Unregistering all transformers call: " + name);
        try {
            transformers.stream().filter(transformer -> transformer.getClass().getName().equals(name)).forEach(transformers::remove);
            transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", name, e);
        }
    }

    /**
     * Call this to remove your transformer, you need to keep track of the instances yourself.
     * @param transformer The transformer you want to un-register
     */
    public static void unRegisterTransformer(IClassTransformer transformer) {
        LOGGER.debug("Unregistering transformer: " + transformer.getClass().getSimpleName());
        try {
            transformers.remove(transformer);
            transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", transformer, e);
        }
    }

    /**
     * We use lambda trick to fill method implementations after the class loader ready
     * @param holder The one and only handler
     */
    static void fillTransformerHolder(TransformerHolder holder) {
        explicitTransformers = new HashMap<>(20);
        transformers = new LinkedList<>();
        holder.runTransformersFunction = (name, transformedName, basicClass) -> {
            for (final IClassTransformer transformer : Collections.unmodifiableList(transformers)) {
                if (VERBOSE) LOGGER.trace("Transforming {} with {}", name, transformer.getClass().getSimpleName());
                basicClass = transformer.transform(name, transformedName, basicClass);
            }
            return basicClass;
        };
        holder.registerTransformerFunction = s -> {
            if (!s.contains(".")) {
                s = s.replace('/', '.');
            }
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).getConstructor().newInstance();
                transformers.add(transformer);
                transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        holder.runExplicitTransformersFunction = (name, basicClass) -> {
            if (explicitTransformers.containsKey(name)) {
                PriorityQueue<IExplicitTransformer> queue = explicitTransformers.get(name);
                if (queue != null) {
                    while (!queue.isEmpty()) {
                        basicClass = queue.poll().transform(basicClass); // We are not doing hotswap, so classes only loaded once. Let's free their memory
                    }
                    explicitTransformers.remove(name); // GC
                }
            }
            return basicClass;
        };
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

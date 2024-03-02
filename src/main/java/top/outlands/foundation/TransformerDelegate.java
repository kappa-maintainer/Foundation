package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.boot.TransformerHolder;
import top.outlands.foundation.trie.TrieNode;

import java.util.*;

import static net.minecraft.launchwrapper.Launch.classLoader;
import static top.outlands.foundation.boot.ActualClassLoader.DEBUG_FINER;
import static top.outlands.foundation.boot.Foundation.LOGGER;
import static top.outlands.foundation.boot.TransformerHolder.*;

/**
 * A delegate to new and old transformer-related methods.
 */
public class TransformerDelegate {
    /**
     * In case you want to control how the transformer is initialized, in which you could <b>new</b> it yourself.
     * @param transformer The transformer
     */
    public static void registerTransformerByInstance(IClassTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * The original getTransformers() was in {@link net.minecraft.launchwrapper.LaunchClassLoader}, but that may cause unwanted classloading.
     * The list itself is a skip list set now, so you can't change it by modifying the return value here.
     * @return list of transformers.
     */
    public static List<IClassTransformer> getTransformers() {
        return transformers.stream().toList();
    }

    /**
     * Checking this every registration is dumb, so let's make another method
     * @param transformer The name transformer instance
     */
    public static void registerRenameTransformer(IClassNameTransformer transformer) {
        LOGGER.debug("Registering rename transformer: " + transformer.getClass().getSimpleName());
        if (renameTransformer == null) {
            renameTransformer = transformer;
        }
        registerTransformerByInstance((IClassTransformer) transformer);
    }

    /**
     * Call this to register an explicit transformer.
     * @param targets Target classes' name.
     * @param className Class name of the transformer.
     */
    public static void registerExplicitTransformer(String[] targets, String className) {
        LOGGER.debug("Registering explicit transformer: " + className);
        try {
            IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(className).newInstance();
            for (var target : targets) {
                TrieNode<PriorityQueue<IExplicitTransformer>> node =  explicitTransformers.getKeyValueNode(target);
                if (node != null) {
                    node.getValue().add(instance);
                } else {
                    var transformerSet = new PriorityQueue<>(Comparator.comparingInt(IExplicitTransformer::getPriority));
                    transformerSet.add(instance);
                    explicitTransformers.put(target, transformerSet);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error registering explicit transformer class {}", className, e);
        }
    }

    /**
     * Same as {@link net.minecraft.launchwrapper.LaunchClassLoader#registerTransformer(String)}
     * @param transformerClassName class name
     */
    public static void registerTransformer(String transformerClassName) {
        LOGGER.debug("Registering transformer: " + transformerClassName);
        try {
            IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(transformerClassName).newInstance();
            transformers.add(transformer);
        } catch (Exception e) {
            LOGGER.error("Error registering transformer class {}", transformerClassName, e);
        }
    }

    /**
     * Call this with class name to remove your transformer.
     * @param name The transformer you want to un-register
     */
    public static void unRegisterTransformer(String name) {
        LOGGER.debug("Unregistering all transformers call: " + name);
        try {
            transformers.stream().filter(transformer -> transformer.getClass().getName().equals(name)).forEach(transformers::remove);
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", name, e);
        }
    }

    /**
     * Call this to remove your transformer, you need to keep track of the instances yourself.
     * @param transformer The transformer you want to un-register
     */
    public static void unRegisterTransformerByInstance(IClassTransformer transformer) {
        LOGGER.debug("Unregistering transformer: " + transformer.getClass().getSimpleName());
        try {
            transformers.remove(transformer);
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", transformer, e);
        }
    }

    /**
     * We use lambda trick to fill method implementations after the class loader ready
     * @param handler The one and only handler
     */
    static void fillTransformerHolder(TransformerHolder handler) {
        handler.runTransformersFunction = (name, transformedName, basicClass, manifest) -> {
            if (DEBUG_FINER) {
                LOGGER.debug("Beginning transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
                for (final IClassTransformer transformer : transformers) {
                    final String transName = transformer.getClass().getName();
                    LOGGER.debug("Before Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                    basicClass = transformer.transform(name, transformedName, basicClass, manifest);
                    LOGGER.debug("After  Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                }
                LOGGER.debug("Ending transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
            } else {
                for (final IClassTransformer transformer : transformers) {
                    basicClass = transformer.transform(name, transformedName, basicClass, manifest);
                }
            }
            return basicClass;
        };
        handler.registerTransformerFunction = s -> {
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).newInstance();
                transformers.add(transformer);
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        handler.runExplicitTransformersFunction = (name, basicClass) -> {
            TrieNode<PriorityQueue<IExplicitTransformer>> node = explicitTransformers.getKeyValueNode(name);
            if (node != null) {
                PriorityQueue<IExplicitTransformer> queue = node.getValue();
                if (queue != null) {
                    while (!queue.isEmpty()) {
                        basicClass = queue.poll().transform(name, basicClass); // We are not doing hotswap, so classes only loaded once. Let's free their memory
                    }
                }
            }
            return basicClass;
        };
        handler.transformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.remapClassName(s);
            }
            return s;
        };
        handler.unTransformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.unmapClassName(s);
            }
            return s;
        };
    }
}

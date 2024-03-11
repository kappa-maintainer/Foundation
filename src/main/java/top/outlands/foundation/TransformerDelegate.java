package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.boot.TransformerHolder;
import top.outlands.foundation.trie.PrefixTrie;
import top.outlands.foundation.trie.TrieNode;

import java.util.*;

import static net.minecraft.launchwrapper.Launch.classLoader;
import static top.outlands.foundation.boot.Foundation.LOGGER;
import static top.outlands.foundation.boot.TransformerHolder.*;

/**
 * A delegate to new and old transformer-related methods.
 */
public class TransformerDelegate {

    /**
     * The original getTransformers() was in {@link net.minecraft.launchwrapper.LaunchClassLoader}, but that may cause unwanted classloading.
     * The list itself is a skip list set now, so you can't change it by modifying the return value here.
     * @return list of transformers.
     */
    public static List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
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
            registerTransformerByInstance((IClassTransformer) transformer);
        }
    }

    /**
     * Call this to register an explicit transformer.
     * @param targets Target classes' name.
     * @param className Class name of the transformer.
     */
    public static void registerExplicitTransformer(String[] targets, String className) {
        try {
            IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(className).getConstructor().newInstance();
            registerExplicitTransformerByInstance(targets, instance);
        } catch (Exception e) {
            LOGGER.error("Error registering explicit transformer class {}", className, e);
        }
    }

    public static void registerExplicitTransformerByInstance(String[] targets, IExplicitTransformer transformer) {
        LOGGER.debug("Registering explicit transformer: " + transformer.getClass().getSimpleName());
        try {
            for (var target : targets) {
                TrieNode<PriorityQueue<IExplicitTransformer>> node =  explicitTransformers.getKeyValueNode(target);
                if (node != null) {
                    node.getValue().add(transformer);
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
     * Same as {@link net.minecraft.launchwrapper.LaunchClassLoader#registerTransformer(String)}
     * @param transformerClassName class name
     */
    public static void registerTransformer(String transformerClassName) {
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
    public static void registerTransformerByInstance(IClassTransformer transformer) {
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
    public static void unRegisterTransformerByInstance(IClassTransformer transformer) {
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
        explicitTransformers = new PrefixTrie<>();
        transformers = new LinkedList<>();
        holder.runTransformersFunction = (name, transformedName, basicClass, manifest) -> {
            for (final IClassTransformer transformer : transformers) {
                LOGGER.trace("Transforming class {} with {}", transformedName, transformer);
                basicClass = transformer.transform(name, transformedName, basicClass);
            }
            return basicClass;
        };
        holder.registerTransformerFunction = s -> {
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).getConstructor().newInstance();
                transformers.add(transformer);
                transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        holder.runExplicitTransformersFunction = (name, basicClass) -> {
            TrieNode<PriorityQueue<IExplicitTransformer>> node = explicitTransformers.getKeyValueNode(name);
            if (node != null) {
                PriorityQueue<IExplicitTransformer> queue = node.getValue();
                if (queue != null) {
                    while (!queue.isEmpty()) {
                        basicClass = queue.poll().transform(basicClass); // We are not doing hotswap, so classes only loaded once. Let's free their memory
                    }
                    node.setValue(null); // GC
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
    }
}

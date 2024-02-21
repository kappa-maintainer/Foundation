package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.boot.TransformerHolder;
import top.outlands.foundation.trie.TrieNode;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static net.minecraft.launchwrapper.Launch.classLoader;
import static top.outlands.foundation.boot.ActualClassLoader.DEBUG_FINER;
import static top.outlands.foundation.boot.Foundation.LOGGER;
import static top.outlands.foundation.boot.TransformerHolder.*;

public class TransformerDelegate {
    /**
     * In case you want to control how the transformer is initialized
     * @param transformer The transformer
     */
    public void registerTransformerInstance(IClassTransformer transformer) {
        transformers.put(transformer.getClass().getName(), transformer);
    }

    public static List<IClassTransformer> getTransformers() {
        return transformers.values().stream().toList();
    }

    public void registerRenameTransformer(IClassNameTransformer transformer) {
        if (renameTransformer == null) {
            renameTransformer = transformer;
        }
    }

    static void fillTransformerHolder(TransformerHolder handler) {
        handler.runTransformersFunction = (name, transformedName, basicClass, manifest) -> {
            if (DEBUG_FINER) {
                LOGGER.debug("Beginning transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
                for (final IClassTransformer transformer : transformers.values()) {
                    final String transName = transformer.getClass().getName();
                    LOGGER.debug("Before Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                    basicClass = transformer.transform(name, transformedName, basicClass, manifest);
                    LOGGER.debug("After  Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                }
                LOGGER.debug("Ending transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
            } else {
                for (final IClassTransformer transformer : transformers.values()) {
                    basicClass = transformer.transform(name, transformedName, basicClass, manifest);
                }
            }
            return basicClass;
        };
        handler.registerTransformerFunction = s -> {
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).newInstance();
                transformers.put(s, transformer);
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        handler.unRegisterTransformerFunction = s -> {
            try {
                transformers.remove(s);
            } catch (Exception e) {
                LOGGER.error("Error removing transformer class {}", s, e);
            }
        };
        handler.runExplicitTransformersFunction = (name, basicClass) -> {
            TrieNode<Set<IExplicitTransformer>> node = explicitTransformers.getKeyValueNode(name);
            if (node != null) {
                Set<IExplicitTransformer> set = node.getValue();
                if (set != null) {
                    for (var transformer : set) {
                        basicClass = transformer.transform(name, basicClass);
                    }
                    set.clear(); // We are not doing hotswap, so classes only loaded once. Let's free their memory
                }
            }
            return basicClass;
        };
        handler.registerExplicitTransformerFunction = (strings, s) -> {
            try {
                IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(s).newInstance();
                for (var target : strings) {
                    TrieNode<Set<IExplicitTransformer>> node =  explicitTransformers.getKeyValueNode(target);
                    if (node != null) {
                        node.getValue().add(instance);
                    } else {
                        var transformerSet = new TreeSet<IExplicitTransformer>();
                        transformerSet.add(instance);
                        explicitTransformers.put(target, transformerSet);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Error registering explicit transformer class {}", s, e);
            }
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

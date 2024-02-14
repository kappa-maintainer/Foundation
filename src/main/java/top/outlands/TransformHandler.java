package top.outlands;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.function.ExplicitTransformerFunction;
import top.outlands.function.TransformerFunction;
import top.outlands.trie.PrefixTrie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;



public class TransformHandler {
    public static final PrefixTrie<Set<IExplicitTransformer>> explicitTransformers = new PrefixTrie<>();
    public static final Map<String, IClassTransformer> transformers = new LinkedHashMap<>(20);

    public static IClassNameTransformer renameTransformer;
    public static List<IClassTransformer> getTransformers() {
        return transformers.values().stream().toList();
    }
    public ExplicitTransformerFunction runExplicitTransformersFunction = ((s, bytes) -> bytes);
    public TransformerFunction runTransformersFunction = ((name, transformedName, basicClass) -> basicClass);
    public Consumer<String> registerTransformerFunction = s -> {};
    public Consumer<String> unRegisterTransformerFunction = s -> {};
    public BiConsumer<String[], String> registerExplicitTransformerFunction = (strings, s) -> {};
    public Function<String, String> transformNameFunction = s -> s;
    public Function<String, String> unTransformNameFunction = s -> s;
    
}
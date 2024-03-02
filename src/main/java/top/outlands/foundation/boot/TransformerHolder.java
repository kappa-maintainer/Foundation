package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.IExplicitTransformer;
import top.outlands.foundation.function.ExplicitTransformerFunction;
import top.outlands.foundation.function.TransformerFunction;
import top.outlands.foundation.trie.PrefixTrie;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;



public class TransformerHolder {
    public static final PrefixTrie<PriorityQueue<IExplicitTransformer>> explicitTransformers = new PrefixTrie<>();
    public static final Set<IClassTransformer> transformers = new ConcurrentSkipListSet<>(Comparator.comparingInt(IClassTransformer::getPriority));

    public static IClassNameTransformer renameTransformer;
    public ExplicitTransformerFunction runExplicitTransformersFunction = ((s, bytes) -> bytes);
    public TransformerFunction runTransformersFunction = ((name, transformedName, basicClass, manifest) -> basicClass);
    public Consumer<String> registerTransformerFunction = s -> {};
    public Function<String, String> transformNameFunction = s -> s;
    public Function<String, String> unTransformNameFunction = s -> s;

}
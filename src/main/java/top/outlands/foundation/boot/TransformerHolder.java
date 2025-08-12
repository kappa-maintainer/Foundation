package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.IExplicitTransformer;
import top.outlands.foundation.function.ExplicitTransformerFunction;
import top.outlands.foundation.function.TransformerFunction;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Do not use. It's public because I am lazy
 */
public class TransformerHolder {
    public static Map<String, SortedSet<IExplicitTransformer>> explicitTransformers = null;
    public static List<IClassTransformer> transformers = null;
    public static IClassNameTransformer renameTransformer;
    public ExplicitTransformerFunction runExplicitTransformersFunction = ((s, bytes) -> bytes);
    public ExplicitTransformerFunction runExplicitTransformersFunctionWithoutRemoval = ((s, bytes) -> bytes);
    public TransformerFunction runTransformersFunction = ((name, transformedName, basicClass) -> basicClass);
    public Consumer<String> registerTransformerFunction = s -> {};
    public Function<String, String> transformNameFunction = s -> s;
    public Function<String, String> unTransformNameFunction = s -> s;
    public Runnable debugPrinter = () -> {};

}
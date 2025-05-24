package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import top.outlands.foundation.IExplicitTransformer;
import top.outlands.foundation.util.ExplicitTransformerList;
import top.outlands.foundation.util.TransformerList;
import top.outlands.foundation.function.ExplicitTransformerFunction;
import top.outlands.foundation.function.TransformerFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

import javassist.CtClass;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Do not use. It's public because I am lazy
 */
public class TransformerHolder {
    public static ExplicitTransformerList<byte[]> explicitClassByteTransformers = null;
    public static ExplicitTransformerList<ClassNode> explicitClassNodeTransformers = null;
    public static ExplicitTransformerList<ClassVisitor> explicitClassVisitorTransformers = null;
    public static ExplicitTransformerList<CtClass> explicitCtClassTransformers = null;

    public static List<IClassTransformer> transformers = null;
    public static IClassNameTransformer renameTransformer;

    public static TransformerList<byte[]> classByteTransformers = null;
    public static TransformerList<ClassNode> classNodeTransformers = null;
    public static TransformerList<ClassVisitor> classVisitorTransformers = null;
    public static TransformerList<CtClass> ctClassTransformers = null;

    public ExplicitTransformerFunction runExplicitTransformersFunction = ((s, bytes) -> bytes);

    public TransformerFunction runTransformersFunction = ((name, transformedName, basicClass) -> basicClass);
    public TransformerFunction runASMTransformersFunction = ((name, transformedName, basicClass) -> basicClass);
    public TransformerFunction runJavassistTransformersFunction = ((name, transformedName, basicClass) -> basicClass);
    
    public Consumer<String> registerTransformerFunction = s -> {};
    public Function<String, String> transformNameFunction = s -> s;
    public Function<String, String> unTransformNameFunction = s -> s;
    public Runnable debugPrinter = () -> {};

}
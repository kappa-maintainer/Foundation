package top.outlands.foundation;

import top.outlands.foundation.function.transformer.ITransformer;
import org.objectweb.asm.tree.ClassNode;

public interface IASMTreeTransformer extends ITransformer<ClassNode> {
}
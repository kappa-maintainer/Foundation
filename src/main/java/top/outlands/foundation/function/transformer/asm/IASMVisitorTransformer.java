package top.outlands.foundation;

import top.outlands.foundation.function.transformer.ITransformer;
import org.objectweb.asm.ClassVisitor;

public interface IASMTreeTransformer extends ITransformer<ClassVisitor> {
}
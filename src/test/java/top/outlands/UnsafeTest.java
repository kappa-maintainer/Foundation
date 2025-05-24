package top.outlands;

import net.lenni0451.reflect.Fields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.outlands.foundation.function.transformer.IExplicitTransformer;
import top.outlands.foundation.boot.UnsafeHolder;
import top.outlands.foundation.transformer.ASMClassWriterTransformer;
import top.outlands.foundation.transformer.ASMVisitorTransformer;

import java.lang.reflect.Field;

public class UnsafeTest {
    private static final IExplicitTransformer transformer = new ASMVisitorTransformer();
    @Test
    public void testUnsafe() {
        Assertions.assertNotNull(UnsafeHolder.UNSAFE);
    }

    @Test
    public void testFinal() {
        System.out.println(System.getProperty("java.specification.version"));
        Field tf = Fields.getDeclaredField(getClass(), "transformer");
        Assertions.assertNotNull(tf);
        Fields.setObject(null, tf, new ASMClassWriterTransformer());
        Assertions.assertInstanceOf(ASMClassWriterTransformer.class, transformer);
    }
}

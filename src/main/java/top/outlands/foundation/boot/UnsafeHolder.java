package top.outlands.foundation.boot;

import net.lenni0451.reflect.Fields;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

public class UnsafeHolder {
    public static Unsafe UNSAFE;
    static {
        try {
            Field unsafe = Fields.getDeclaredField(Unsafe.class, "theUnsafe");
            UNSAFE = Fields.getObject(null, unsafe);
        } catch (Throwable t) {
            Foundation.LOGGER.debug(t);
            Foundation.LOGGER.warn("Can't get unsafe from JVM Driver, trying VarHandle");
            try {
                VarHandle handle = MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class);
                UNSAFE = (Unsafe) handle.get();
            } catch (NoSuchFieldException|IllegalAccessException e) {
                Foundation.LOGGER.debug(e);
                Foundation.LOGGER.fatal("Can't get unsafe in any way");
            }
        }
    }
}

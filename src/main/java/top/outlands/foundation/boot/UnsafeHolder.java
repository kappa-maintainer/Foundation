package top.outlands.foundation.boot;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

public class UnsafeHolder {
    public static Unsafe UNSAFE;
    static {
        try {
            Field unsafe = JVMDriverHolder.findField(Unsafe.class, "theUnsafe");
            JVMDriverHolder.DRIVER.setAccessible(unsafe, true);
            UNSAFE = (Unsafe) unsafe.get(null);
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

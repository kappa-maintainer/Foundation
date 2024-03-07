package top.outlands.foundation.boot;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class UnsafeHolder {
    public static Unsafe UNSAFE;
    static {
        try {
            UNSAFE = (Unsafe) JVMDriverHolder.findField(Unsafe.class, "theUnsafe").get(null);
        } catch (Throwable t) {
            Foundation.LOGGER.warn("Can't get unsafe from JVM Driver, trying VarHandle");
            try {
                VarHandle handle = MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class);
                UNSAFE = (Unsafe) handle.get();
            } catch (NoSuchFieldException|IllegalAccessException e) {
                Foundation.LOGGER.fatal("Can't get unsafe in any way");
            }
        }
    }
}

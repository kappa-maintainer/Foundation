package top.outlands;

import net.lenni0451.reflect.JavaBypass;
import net.lenni0451.reflect.Modules;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class ReflectTest {
    @Test
    public void reflectionTest() throws ClassNotFoundException {
        JavaBypass.clearReflectionFilter();
        Modules.openBootModule();
        //Modules.enableNativeAccessToAllUnnamed();
        try {
            System.out.println(PrivateHolder.class.getDeclaredField("record"));
            System.out.println(JavaBypass.getTrustedLookup().findVarHandle(Field.class, "modifiers", int.class).varType().getModifiers());
            System.out.println(Field.class.getDeclaredField("modifiers"));
        } catch (Exception e) {
            System.err.println(e);
        }
    
    }
}

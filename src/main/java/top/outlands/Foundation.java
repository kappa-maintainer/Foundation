package top.outlands;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.burningwave.core.classes.Modules;

import java.lang.reflect.Method;
import java.util.Arrays;

import static top.outlands.JVMDriver.DRIVER;

public class Foundation {
    public static Logger LOGGER = LogManager.getLogger("Foundation");

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new UCEHandler());
        try {
            breakModuleAndReflection();
            Object handler = DRIVER.allocateInstance(Class.forName("top.outlands.LaunchHandler", true, LaunchClassLoader.getInstance()));
            Method launch = handler.getClass().getMethod("launch", String[].class);
            launch.invoke(handler, (Object) args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void breakModuleAndReflection() {
        Modules.create().exportAllToAll();
        Class<?> reflection = DRIVER.getClassByName("jdk.internal.reflect.Reflection",true, Foundation.class.getClassLoader(), Foundation.class );
        DRIVER.setFieldValue(null, Arrays.stream(DRIVER.getDeclaredFields(reflection)).filter(field -> field.getName().equals("fieldFilterMap")).findFirst().get(), null);
        DRIVER.setFieldValue(null, Arrays.stream(DRIVER.getDeclaredFields(reflection)).filter(field -> field.getName().equals("methodFilterMap")).findFirst().get(), null);
        DRIVER.setFieldValue(Class.class, Arrays.stream(DRIVER.getDeclaredFields(Class.class)).filter(field -> field.getName().equals("reflectionData")).findFirst().get(), null);
        
    }
}

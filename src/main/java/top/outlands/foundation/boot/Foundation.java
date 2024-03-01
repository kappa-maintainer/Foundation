package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Logger;
import zone.rong.imaginebreaker.ImagineBreaker;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static top.outlands.foundation.boot.JVMDriver.DRIVER;

public class Foundation {
    public static Logger LOGGER = new EmptyLogger();
    public static final Set<String> OUTDATED_VISITOR = new HashSet<>();

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Foundation.LOGGER.error(thread, throwable));
        try {
            breakModuleAndReflection();
            Object handler = DRIVER.allocateInstance(Class.forName("top.outlands.foundation.LaunchHandler", true, LaunchClassLoader.getInstance()));
            Method launch = handler.getClass().getMethod("launch", String[].class);
            launch.invoke(handler, (Object) args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void breakModuleAndReflection() {
        ImagineBreaker.openBootModules();
        ImagineBreaker.wipeFieldFilters();
        ImagineBreaker.wipeMethodFilters();
    }
}

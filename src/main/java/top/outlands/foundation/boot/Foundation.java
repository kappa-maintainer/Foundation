package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Logger;
import zone.rong.imaginebreaker.ImagineBreaker;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class Foundation {
    public static Logger LOGGER = new EmptyLogger();
    private static final Set<String> OUTDATED_VISITOR = new HashSet<>();

    /**
     * For ASM outdated visitor logging, <b>DO NOT USE<b/>
     * @param name the name of visitor
     */
    public static void add(String name) {
        if (!OUTDATED_VISITOR.contains(name)) {
            LOGGER.warn(name + " can't handle Java 21 class, please update the mod!");
            OUTDATED_VISITOR.add(name);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Foundation.LOGGER.error(thread, throwable));
        try {
            breakModuleAndReflection();
            Object handler = Class.forName("top.outlands.foundation.LaunchHandler", true, LaunchClassLoader.getInstance()).getConstructor().newInstance();
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

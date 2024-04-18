package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.imaginebreaker.ImagineBreaker;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class Foundation {
    public static Logger LOGGER = System.getProperty("java.system.class.loader") == null ? LogManager.getLogger("Foundation") : new EmptyLogger();
    private static final Set<String> OUTDATED_VISITOR = new HashSet<>();

    /**
     * For ASM outdated visitor logging, <b>DO NOT USE<b/>
     * @param name the name of visitor
     */
    public static void add(String name) {
        if (!OUTDATED_VISITOR.contains(name)) {
            LOGGER.warn("{} can't handle Java 21 class, please port the mod (if you are the author)!", name);
            OUTDATED_VISITOR.add(name);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Foundation.LOGGER.error(thread, throwable));
        try {
            breakModuleAndReflection();
            if (Launch.classLoader == null) {
                Launch.classLoader = new LaunchClassLoader(ClassLoader.getSystemClassLoader());
                LOGGER.info("System ClassLoader is AppCL");
            } else {
                LOGGER = LogManager.getLogger("Foundation");
                LOGGER.info("System ClassLoader is LCL");
            }
            Object handler = Class.forName("top.outlands.foundation.LaunchHandler", true, Launch.classLoader).getConstructor().newInstance();
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

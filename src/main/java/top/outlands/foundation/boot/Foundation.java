package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zone.rong.imaginebreaker.ImagineBreaker;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;

public class Foundation {
    public static Logger LOGGER = LogManager.getLogger("Foundation");
    private static final Set<String> OUTDATED_VISITOR = new HashSet<>();

    /**
     * For ASM outdated visitor logging, <b>DO NOT USE<b/>
     * @param name the name of visitor
     */
    public static void add(String name) {
        if (!OUTDATED_VISITOR.contains(name)) {
            LOGGER.debug("{} can't handle Java 21 class, please port the mod (if you are the author)!", name);
            OUTDATED_VISITOR.add(name);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Foundation.LOGGER.error(thread, throwable));
        try {
            breakModuleAndReflection();
            if (Launch.classLoader == null) {
                boolean loadsall = Boolean.parseBoolean(System.getProperty("foundation.loadsall", "false"));
                Launch.classLoader = loadsall
                    ? new LoadsAllClassLoader(ClassLoader.getSystemClassLoader())
                    : new LaunchClassLoader(ClassLoader.getSystemClassLoader());
            }
            Object handler = Class.forName("top.outlands.foundation.LaunchHandler", true, Launch.classLoader).getConstructor().newInstance();
            MethodHandles.lookup()
                .findVirtual(handler.getClass(), "launch", MethodType.methodType(void.class, String[].class))
                .invoke(handler, (Object) args);
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

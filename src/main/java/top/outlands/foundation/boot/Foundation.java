package top.outlands.foundation.boot;

import net.lenni0451.reflect.JavaBypass;
import net.lenni0451.reflect.Modules;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Foundation extends AbstractExecutorService {
    private static Foundation INSTANCE = null;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public static Logger LOGGER = LogManager.getLogger("Foundation");
    private static final Set<String> OUTDATED_VISITOR = new HashSet<>();

    public static Foundation instance() {
        return INSTANCE;
    }

    /**
     * For ASM outdated visitor logging, <b>DO NOT USE<b/>
     *
     * @param name the name of visitor
     */
    @ApiStatus.Internal
    public static void add(String name) {
        if (!OUTDATED_VISITOR.contains(name)) {
            LOGGER.debug("{} can't handle Java 21 class, please port the mod (if you are the author)!", name);
            OUTDATED_VISITOR.add(name);
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> Foundation.LOGGER.error(thread, throwable));

        // Copied from RFB
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            System.setProperty("java.awt.headless", "true");
            GraphicsEnvironment.getLocalGraphicsEnvironment();
            Toolkit.getDefaultToolkit().getDesktopProperty("awt.mouse.numButtons");
        }

        INSTANCE = new Foundation();
        Thread.currentThread().setName("Main-EventLoop");

        try {
            breakModuleAndReflection();
            if (Launch.classLoader == null) {
                boolean loadsall = Boolean.parseBoolean(System.getProperty("foundation.loadsall", "false"));
                Launch.classLoader = loadsall
                    ? new LoadsAllClassLoader(ClassLoader.getSystemClassLoader())
                    : new LaunchClassLoader(ClassLoader.getSystemClassLoader());
            }

            final Thread realMain = new Thread(
                () -> {
                    try {
                        Object handler = Class.forName("top.outlands.foundation.LaunchHandler", true, Launch.classLoader)
                            .getConstructor()
                            .newInstance();
                        MethodHandles.lookup()
                            .findVirtual(handler.getClass(), "launch", MethodType.methodType(void.class, String[].class))
                            .invoke(handler, (Object) args);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    } finally {
                        INSTANCE.isRunning.set(false);
                        INSTANCE.execute(() -> {
                        });
                    }
                },
                "Main-Foundation"
            );
            realMain.start();
            INSTANCE.eventLoop();
            while (realMain.isAlive()) {
                try {
                    realMain.join();
                } catch (InterruptedException _) {
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void breakModuleAndReflection() throws ClassNotFoundException {
        JavaBypass.clearReflectionFilter();
        Modules.openBootModule();
        Modules.enableNativeAccessToAllUnnamed();
    }

    private void eventLoop() {
        while (isRunning.get()) {
            final Runnable task;
            try {
                task = tasks.take();
            } catch (Throwable e) {
                continue;
            }
            if (task == null) {
                continue;
            }
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("Caught exception on the RFB main loop executor:", t);
            }
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    @NonNull
    public List<Runnable> shutdownNow() {
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit timeUnit) throws InterruptedException {
        Thread.sleep(timeUnit.toMillis(timeout));
        return false;
    }

    @Override
    public void execute(@NonNull Runnable runnable) {
        if (runnable != null) {
            while (true) {
                try {
                    tasks.put(runnable);
                    break;
                } catch (InterruptedException _) {
                }
            }
        }
    }
}

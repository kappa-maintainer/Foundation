package net.minecraft.launchwrapper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static top.outlands.foundation.boot.Foundation.LOGGER;

public class LogWrapper {
    public static LogWrapper log = new LogWrapper();
    private Logger myLog = LOGGER;

    private static boolean configured;

    private static void configureLogging() {
    }

    public static void retarget(Logger to) {
    }
    public static void log(String logChannel, Level level, String format, Object... data) {
        LOGGER.log(level, "[{}] {}", logChannel, String.format(String.format(format, data)));
    }

    public static void log(Level level, String format, Object... data) {
        LOGGER.log(level, String.format(format, data));
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data) {
        LOGGER.log(level, "[{}] {}", logChannel, String.format(String.format(format, data)), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        LOGGER.log(level, String.format(format, data));
    }

    public static void severe(String format, Object... data) {
        LOGGER.error(String.format(format, data));
    }

    public static void warning(String format, Object... data) {
        LOGGER.warn(String.format(format, data));
    }

    public static void info(String format, Object... data) {
        LOGGER.info(String.format(format, data));
    }

    public static void fine(String format, Object... data) {
        LOGGER.debug(String.format(format, data));
    }

    public static void finer(String format, Object... data) {
        LOGGER.trace(String.format(format, data));
    }

    public static void finest(String format, Object... data) {
        LOGGER.trace(String.format(format, data));
    }

    public static void makeLog(String logChannel) {
    }
}
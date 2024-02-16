package top.outlands.foundation;

import top.outlands.foundation.boot.Foundation;

public class UCEHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Foundation.LOGGER.error(thread, throwable);
    }
}

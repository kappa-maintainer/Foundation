package top.outlands;

public class UCEHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Foundation.LOGGER.error(thread, throwable);
    }
}

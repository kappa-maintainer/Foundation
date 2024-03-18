package top.outlands.foundation;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

public class DummyLaunchClassLoader {
    public static final DummyLaunchClassLoader INSTANCE = new DummyLaunchClassLoader();
    public static final int BUFFER_SIZE = 1 << 12;
    private List<URL> sources;
    private ClassLoader parent = getClass().getClassLoader();

    private List<IClassTransformer> transformers = new ArrayList<IClassTransformer>(2);
    private Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<String, Class<?>>();
    private Set<String> invalidClasses = new HashSet<String>(1000);

    private Set<String> classLoaderExceptions = new HashSet<String>();
    private Set<String> transformerExceptions = new HashSet<String>();
    private Map<String,byte[]> resourceCache = new ConcurrentHashMap<String,byte[]>(1000);
    private Set<String> negativeResourceCache = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private Map<Package, Manifest> packageManifests = null;
    private static Manifest EMPTY = new Manifest();

    private IClassNameTransformer renameTransformer;

    private final ThreadLocal<byte[]> loadBuffer = new ThreadLocal<byte[]>();

    private static final String[] RESERVED_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("legacy.debugClassLoading", "false"));
    private static final boolean DEBUG_FINER = DEBUG && Boolean.parseBoolean(System.getProperty("legacy.debugClassLoadingFiner", "false"));
    private static final boolean DEBUG_SAVE = DEBUG && Boolean.parseBoolean(System.getProperty("legacy.debugClassLoadingSave", "false"));
    private static File tempFolder = null;

    public DummyLaunchClassLoader(URL[] sources) {
    }
    public DummyLaunchClassLoader(){}
    public void registerTransformer(String transformerClassName) {
        Launch.classLoader.registerTransformer(transformerClassName);
    }

    public Class<?> findClass(final String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }

    private void saveTransformedClass(final byte[] data, final String transformedName) {
    }

    private String untransformName(final String name) {
        return Launch.classLoader.untransformName(name);
    }

    private String transformName(final String name) {
        return Launch.classLoader.transformName(name);
    }

    private boolean isSealed(final String path, final Manifest manifest) {
        return false;
    }

    private URLConnection findCodeSourceConnectionFor(final String name) {
        return null;
    }

    private byte[] runTransformers(final String name, final String transformedName, byte[] basicClass) {
        return basicClass;
    }

    public void addURL(final URL url) {
        Launch.classLoader.addURL(url);
    }

    public List<URL> getSources() {
        return sources;
    }

    private byte[] readFully(InputStream stream) {
        return null;
    }

    private byte[] getOrCreateBuffer() {
        byte[] buffer = loadBuffer.get();
        if (buffer == null) {
            loadBuffer.set(new byte[BUFFER_SIZE]);
            buffer = loadBuffer.get();
        }
        return buffer;
    }

    public List<IClassTransformer> getTransformers() {
        return TransformerDelegate.getTransformers();
    }

    public void addClassLoaderExclusion(String toExclude) {
    }

    public void addTransformerExclusion(String toExclude) {
    }

    public byte[] getClassBytes(String name) {
        return null;
    }

    private static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void clearNegativeEntries(Set<String> entriesToClear) {
    }
}

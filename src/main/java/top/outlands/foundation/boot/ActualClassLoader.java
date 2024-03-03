package top.outlands.foundation.boot;

import net.minecraft.launchwrapper.Launch;
import top.outlands.foundation.trie.PrefixTrie;
import top.outlands.foundation.trie.TrieNode;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static top.outlands.foundation.boot.Foundation.LOGGER;

public class ActualClassLoader extends URLClassLoader {
    
    public static final int BUFFER_SIZE = 1 << 12;
    private final List<URL> sources;
    private ClassLoader parent = getClass().getClassLoader();
    public static final PrefixTrie<Boolean> classLoaderExceptions = new PrefixTrie<>();
    public static final PrefixTrie<Boolean> transformerExceptions = new PrefixTrie<>();
    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();
    private final Set<String> invalidClasses = new HashSet<>(1024);

    private final Map<String,byte[]> resourceCache = new ConcurrentHashMap<>(1024);
    private final Set<String> negativeResourceCache = ConcurrentHashMap.newKeySet();

    private final ThreadLocal<byte[]> loadBuffer = new ThreadLocal<>();

    private static final String[] RESERVED_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("foundation.debug", "false"));
    private static final boolean DEBUG_SAVE = DEBUG && Boolean.parseBoolean(System.getProperty("foundation.debugSave", "false"));
    private static File dumpSubDir;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-");
    static TransformerHolder transformerHolder = new TransformerHolder();
    
    public ActualClassLoader(URL[] sources) {
        this(sources, null);
    }

    public ActualClassLoader(URL[] sources, ClassLoader loader) {
        super(sources, loader);
        if (loader != null) {
            parent = loader;
        }
        this.sources = new ArrayList<>(Arrays.asList(sources));

        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("jdk.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.apache.");
        addClassLoaderExclusion("org.burningwave.");
        addClassLoaderExclusion("com.sun.");
        addClassLoaderExclusion("net.minecraft.launchwrapper.LaunchClassLoader");
        addClassLoaderExclusion("net.minecraft.launchwrapper.Launch");
        addClassLoaderExclusion("top.outlands.foundation.boot.");
        addClassLoaderExclusion("top.outlands.foundation.function.");
        addClassLoaderExclusion("top.outlands.foundation.trie.");
        addClassLoaderExclusion("io.github.toolfactory.jvm.");
        addClassLoaderExclusion("org.burningwave.");
        addClassLoaderExclusion("javassist.");
        addClassLoaderExclusion("com.google.");
        if (DEBUG_SAVE) {
            File dumpDir = new File(Launch.minecraftHome, "CLASS_DUMP");

            if (!dumpDir.exists()) {
                dumpDir.mkdirs();
            }
            int i = 0;
            do {
                i++;
                dumpSubDir = new File(dumpDir, dateTimeFormatter.format(LocalDateTime.now()) + i);
            } while (dumpSubDir.exists());
            dumpSubDir.mkdirs();
        }
    }

    public TransformerHolder getTransformerHolder() {
        return transformerHolder;
    }

    public void registerTransformer(String transformerClassName) {
        LOGGER.debug("Registering transformer: " + transformerClassName);
        transformerHolder.registerTransformerFunction.accept(transformerClassName);
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        if (invalidClasses.contains(name)) {
            throw new ClassNotFoundException(name);
        }
        TrieNode<Boolean> node = classLoaderExceptions.getFirstKeyValueNode(name);
        if (node != null && node.getValue()) {
            return parent.loadClass(name);
        }

        if (cachedClasses.containsKey(name)) {
            return cachedClasses.get(name);
        }

        byte[] transformedClass;


        try {
            final String transformedName = transformName(name);
            if (cachedClasses.containsKey(transformedName)) {
                return cachedClasses.get(transformedName);
            }

            final String untransformedName = unTransformName(name);

            final int lastDot = untransformedName.lastIndexOf('.');
            final String packageName = lastDot == -1 ? "" : untransformedName.substring(0, lastDot);
            final String fileName = untransformedName.replace('.', '/').concat(".class");
            URLConnection urlConnection = findCodeSourceConnectionFor(fileName);

            CodeSigner[] signers = null;
            Manifest manifest = null;
            if (lastDot > -1 && !untransformedName.startsWith("net.minecraft.")) {
                if (urlConnection instanceof JarURLConnection jarURLConnection) {
                    final JarFile jarFile = jarURLConnection.getJarFile();

                    if (jarFile != null && jarFile.getManifest() != null) {
                        manifest = jarFile.getManifest();
                        final JarEntry entry = jarFile.getJarEntry(fileName);

                        Package pkg = getPackage(packageName);
                        getClassBytes(untransformedName);
                        signers = entry.getCodeSigners();
                        if (pkg == null) {
                            definePackage(packageName, manifest, jarURLConnection.getJarFileURL());
                        } else {
                            if (pkg.isSealed() && !pkg.isSealed(jarURLConnection.getJarFileURL())) {
                                LOGGER.warn("The jar file %s is trying to seal already secured path %s", jarFile.getName(), packageName);
                            } else if (isSealed(packageName, manifest)) {
                                LOGGER.warn("The jar file %s has a security seal for path %s, but that path is defined and not secure", jarFile.getName(), packageName);
                            }
                        }
                    }
                } else {
                    Package pkg = getPackage(packageName);
                    if (pkg == null) {
                        definePackage(packageName, null, null, null, null, null, null, null);
                    } else if (pkg.isSealed()) {
                        LOGGER.warn("The URL %s is defining elements for sealed path %s", urlConnection.getURL(), packageName);
                    }
                }
            }
            node = transformerExceptions.getFirstKeyValueNode(name);
            if (node != null && node.getValue()) {
                try {
                    transformedClass = getClassBytes(name);
                    transformedClass = runExplicitTransformers(transformedName, transformedClass);
                    final CodeSource codeSource = urlConnection == null ? null : new CodeSource(urlConnection.getURL(), signers);
                    final Class<?> clazz = super.defineClass(name, transformedClass, 0, transformedClass.length, codeSource);
                    cachedClasses.put(name, clazz);
                    if (DEBUG_SAVE) {
                        saveClassBytes(transformedClass, transformedName);
                    }
                    return clazz;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                
            }
            

            transformedClass = runExplicitTransformers(transformedName, runTransformers(untransformedName, transformedName, getClassBytes(untransformedName), manifest));
            if (DEBUG_SAVE) {
                saveClassBytes(transformedClass, transformedName);
            }

            final CodeSource codeSource = urlConnection == null ? null : new CodeSource(urlConnection.getURL(), signers);
            
            final Class<?> clazz = defineClass(transformedName, transformedClass, 0, transformedClass.length, codeSource);
            cachedClasses.put(transformedName, clazz);
            return clazz;
        } catch (Throwable e) {
                invalidClasses.add(name);
                LOGGER.debug("Exception encountered attempting classloading of {}", name, e);
                throw new ClassNotFoundException(name, e);
            
        }
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    public void saveClassBytes(final byte[] data, final String transformedName) {
        final File outFile = new File(dumpSubDir, transformedName.replace('.', File.separatorChar) + ".class");
        final File outDir = outFile.getParentFile();

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        if (outFile.exists()) {
            outFile.delete();
        }

        try {
            LOGGER.debug("Saving transformed class \"%s\" to \"%s\"", transformedName, outFile.getAbsolutePath().replace('\\', '/'));

            final OutputStream output = new FileOutputStream(outFile);
            output.write(data);
            output.close();
        } catch (IOException ex) {
            LOGGER.warn("Could not save transformed class \"%s\"", transformedName, ex);
        }
    }

    protected String unTransformName(String name) {
        name = transformerHolder.unTransformNameFunction.apply(name);
        return name;
    }

    protected String transformName(String name) {
        name = transformerHolder.transformNameFunction.apply(name);
        return name;
    }

    protected boolean isSealed(final String path, final Manifest manifest) {
        Attributes attributes = manifest.getAttributes(path);
        String sealed = null;
        if (attributes != null) {
            sealed = attributes.getValue(Attributes.Name.SEALED);
        }

        if (sealed == null) {
            attributes = manifest.getMainAttributes();
            if (attributes != null) {
                sealed = attributes.getValue(Attributes.Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    protected URLConnection findCodeSourceConnectionFor(final String name) {
        final URL resource = findResource(name);
        if (resource != null) {
            try {
                return resource.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    protected byte[] runTransformers(final String name, final String transformedName, byte[] basicClass, Manifest manifest) {
        basicClass = transformerHolder.runTransformersFunction.apply(name, transformedName, basicClass, manifest);
        return basicClass;
    }

    protected byte[] runExplicitTransformers(final String transformedName, byte[] basicClass) {
        basicClass = transformerHolder.runExplicitTransformersFunction.apply(transformedName, basicClass);
        return basicClass;
    }

    @Override
    public void addURL(final URL url) {
        super.addURL(url);
        sources.add(url);
    }

    public List<URL> getSources() {
        return sources;
    }

    protected byte[] readFully(InputStream stream) {
        try {
            byte[] buffer = getOrCreateBuffer();

            int read;
            int totalLength = 0;
            while ((read = stream.read(buffer, totalLength, buffer.length - totalLength)) != -1) {
                totalLength += read;

                // Extend our buffer
                if (totalLength >= buffer.length - 1) {
                    byte[] newBuffer = new byte[buffer.length + BUFFER_SIZE];
                    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                    buffer = newBuffer;
                }
            }

            final byte[] result = new byte[totalLength];
            System.arraycopy(buffer, 0, result, 0, totalLength);
            return result;
        } catch (Throwable t) {
            LOGGER.warn("Problem loading class", t);
            return new byte[0];
        }
    }

    protected byte[] getOrCreateBuffer() {
        byte[] buffer = loadBuffer.get();
        if (buffer == null) {
            loadBuffer.set(new byte[BUFFER_SIZE]);
            buffer = loadBuffer.get();
        }
        return buffer;
    }
    public void addClassLoaderExclusion(String toExclude) {
        LOGGER.debug("Adding classloader exclusion " + toExclude);
        classLoaderExceptions.put(toExclude, true);
    }

    public void addTransformerExclusion(String toExclude) {
        LOGGER.debug("Adding transformer exclusion " + toExclude);
        transformerExceptions.put(toExclude, true);
    }

    public byte[] getClassBytes(String name) throws IOException {
        if (negativeResourceCache.contains(name)) {
            return null;
        } else if (resourceCache.containsKey(name)) {
            return resourceCache.get(name);
        }
        if (name.indexOf('.') == -1) {
            for (final String reservedName : RESERVED_NAMES) {
                if (name.toUpperCase(Locale.ENGLISH).startsWith(reservedName)) {
                    final byte[] data = getClassBytes("_" + name);
                    if (data != null) {
                        resourceCache.put(name, data);
                        return data;
                    }
                }
            }
        }

        InputStream classStream = null;
        try {
            final String resourcePath = name.replace('.', '/').concat(".class");
            final URL classResource = findResource(resourcePath);

            if (classResource == null) {
                LOGGER.debug("Failed to find class resource {}", resourcePath);
                negativeResourceCache.add(name);
                return null;
            }
            classStream = classResource.openStream();

            LOGGER.debug("Loading class {} from resource {}", name, classResource.toString());
            final byte[] data = readFully(classStream);
            resourceCache.put(name, data);
            return data;
        } finally {
            closeSilently(classStream);
        }
    }

    public Map<String, Class<?>> getCachedClasses() {
        return cachedClasses;
    }

    public Set<String> getInvalidClasses() {
        return invalidClasses;
    }

    public Class<?> defineClass(String name, ByteBuffer buffer) {
        return defineClass(name, buffer, (ProtectionDomain) null);
    }

    public Class<?> defineClass(String name, byte[] buffer) {
        return defineClass(name, buffer, 0, buffer.length);
    }

    public boolean isClassLoaded(String name) {
        return findLoadedClass(name) != null;
    }

    protected static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void clearNegativeEntries(Set<String> entriesToClear) {
        negativeResourceCache.removeAll(entriesToClear);
    }
}

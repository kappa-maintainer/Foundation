package net.minecraft.launchwrapper;

import top.outlands.foundation.boot.ActualClassLoader;
import top.outlands.foundation.boot.Foundation;
import top.outlands.foundation.boot.TransformerHolder;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

public class LaunchClassLoader extends ActualClassLoader {
    private Set<String> classLoaderExceptions = new HashSet<String>();
    private Set<String> transformerExceptions = new HashSet<String>();
    /**
     * FoamFix (and many other mods) are still using these even some of them have long gone from upstream
     */
    private Map<Package, Manifest> packageManifests = null;
    private static Manifest EMPTY = new Manifest();
    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();
    private final Set<String> invalidClasses = new HashSet<>(0);

    private final Map<String,byte[]> resourceCache = new ConcurrentHashMap<>(0);
    private final Set<String> negativeResourceCache = ConcurrentHashMap.newKeySet();
    public LaunchClassLoader(URL[] sources) {
        super(sources, LaunchClassLoader.class.getClassLoader());
        Launch.classLoader = this;
    }
    
    public LaunchClassLoader(ClassLoader loader) {
        super(getUCP(), loader);
        Launch.classLoader = this;
    }
    
    private static URL[] getUCP(){
        try {
            Class<?> loader = MethodHandles.lookup().findClass("jdk.internal.loader.BuiltinClassLoader");
            Class<?> ucp = MethodHandles.lookup().findClass("jdk.internal.loader.URLClassPath");
            VarHandle ucpField = MethodHandles.privateLookupIn(loader, MethodHandles.lookup())
                    .findVarHandle(loader, "ucp", ucp);
            VarHandle pathsField = MethodHandles.privateLookupIn(ucp, MethodHandles.lookup())
                    .findVarHandle(ucp, "path", ArrayList.class);
            @SuppressWarnings("unchecked")
            ArrayList<URL> urls = (ArrayList<URL>) pathsField.get(ucpField.get(Launch.appClassLoader));
            return urls.toArray(new URL[0]);
        } catch (Throwable t) {
            Foundation.LOGGER.warn("Failed to get ucp with reflection, trying another way");
            String[] classpaths = System.getProperty("java.class.path").split(File.pathSeparator);
            List<URL> urls = new ArrayList<>();
            try {
                for (String classpath : classpaths) {
                    urls.add(new File(classpath).toURI().toURL());
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return urls.toArray(new URL[0]);
        }
    }

    /// binary compat:

    /**
     * CCL is calling this
     */
    public byte[] runTransformers(final String name, final String transformedName, byte[] basicClass) {
        return super.runTransformers(name, transformedName, basicClass);
    }

    @Override
    public void registerTransformer(final String transformerClassName) {
        super.registerTransformer(transformerClassName);
    }

    public List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(TransformerHolder.transformers);
    }

    @Override
    public byte[] getClassBytes(final String name) throws IOException {
        return super.getClassBytes(name);
    }

    @Override
    public void addClassLoaderExclusion(final String toExclude) {
        super.addClassLoaderExclusion(toExclude);
    }

    @Override
    public void addTransformerExclusion(final String toExclude) {
        super.addTransformerExclusion(toExclude);
    }

    @Override
    public void clearNegativeEntries(Set<String> entriesToClear) {
        super.clearNegativeEntries(entriesToClear);
    }
}

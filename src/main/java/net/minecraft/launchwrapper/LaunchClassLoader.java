package net.minecraft.launchwrapper;

import top.outlands.foundation.boot.ActualClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

public class LaunchClassLoader extends ActualClassLoader {
    public static LaunchClassLoader getInstance() {
        return instance;
    }
    private static LaunchClassLoader instance;
    /**
     * FoamFix is still cleaning this even it has long gone from upstream
     */
    private Map<Package, Manifest> packageManifests = null;
    public LaunchClassLoader(URL[] sources) {
        super(sources, LaunchClassLoader.class.getClassLoader());
        instance = this;
    }
    
    public LaunchClassLoader(ClassLoader loader) {
        super(getUCP(), loader);
        instance = this;
    }
    
    private static URL[] getUCP(){
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

package net.minecraft.launchwrapper;

import top.outlands.ActualClassLoader;
import top.outlands.LaunchHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static top.outlands.JVMDriver.DRIVER;

public class LaunchClassLoader extends ActualClassLoader {
    public static LaunchClassLoader getInstance() {
        return instance;
    }
    private static LaunchClassLoader instance;
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

package net.minecraft.launchwrapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

public class Launch {
    public static File minecraftHome;
    public static File assetsDir;
    public static Map<String,Object> blackboard;



    public static LaunchClassLoader classLoader;
    
    public static final ClassLoader appClassLoader = LaunchClassLoader.class.getClassLoader(); 
    
}

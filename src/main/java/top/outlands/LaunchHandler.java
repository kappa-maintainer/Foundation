package top.outlands;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import top.outlands.trie.TrieNode;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static net.minecraft.launchwrapper.Launch.assetsDir;
import static net.minecraft.launchwrapper.Launch.blackboard;
import static net.minecraft.launchwrapper.Launch.classLoader;
import static net.minecraft.launchwrapper.Launch.minecraftHome;
import static top.outlands.ActualClassLoader.DEBUG_FINER;
import static top.outlands.TransformHandler.explicitTransformers;
import static top.outlands.TransformHandler.renameTransformer;
import static top.outlands.TransformHandler.transformers;
import static top.outlands.Foundation.LOGGER;

public class LaunchHandler {


    public void launch(String[] args) {
        LOGGER = LogManager.getLogger("Foundation");
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSpec<String> profileOption = parser.accepts("version", "The version we launched with").withRequiredArg();
        final OptionSpec<File> gameDirOption = parser.accepts("gameDir", "Alternative game directory").withRequiredArg().ofType(File.class);
        final OptionSpec<File> assetsDirOption = parser.accepts("assetsDir", "Assets directory").withRequiredArg().ofType(File.class);
        final OptionSpec<String> tweakClassOption = parser.accepts("tweakClass", "Tweak class(es) to load").withRequiredArg();
        final OptionSpec<String> nonOption = parser.nonOptions();

        final OptionSet options = parser.parse(args);
        minecraftHome = options.valueOf(gameDirOption);
        assetsDir = options.valueOf(assetsDirOption);
        final String profileName = options.valueOf(profileOption);
        final List<String> tweakClassNames = new ArrayList<>(options.valuesOf(tweakClassOption));

        final List<String> argumentList = new ArrayList<>();
        blackboard = new HashMap<>();
        classLoader = (LaunchClassLoader) LaunchHandler.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        fillTransformHandler(classLoader.getTransformHandler());
        
        classLoader.registerExplicitTransformer(new String[]{
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor"
        }, "top.outlands.ASMTransformer");
        try {
            classLoader.findClass("org.objectweb.asm.FieldVisitor");
            classLoader.findClass("org.objectweb.asm.ClassVisitor");
            classLoader.findClass("org.objectweb.asm.MethodVisitor");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Can't find ASM", e);
        }
        classLoader.addTransformerExclusion("org.objectweb.asm.");

        // any 'discovered' tweakers from their preferred mod loading mechanism
        // By making this object discoverable and accessible it's possible to perform
        // things like cascading of tweakers
        blackboard.put("TweakClasses", tweakClassNames);

        // This argument list will be constructed from all tweakers. It is visible here so
        // all tweakers can figure out if a particular argument is present, and add it if not
        blackboard.put("ArgumentList", argumentList);

        // This is to prevent duplicates - in case a tweaker decides to add itself or something
        final Set<String> allTweakerNames = new HashSet<String>();
        // The 'definitive' list of tweakers
        final List<ITweaker> allTweakers = new ArrayList<ITweaker>();
        try {
            final List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
            // The list of tweak instances - may be useful for interoperability
            blackboard.put("Tweaks", tweakers);
            // The primary tweaker (the first one specified on the command line) will actually
            // be responsible for providing the 'main' name and generally gets called first
            ITweaker primaryTweaker = null;
            // This loop will terminate, unless there is some sort of pathological tweaker
            // that reinserts itself with a new identity every pass
            // It is here to allow tweakers to "push" new tweak classes onto the 'stack' of
            // tweakers to evaluate allowing for cascaded discovery and injection of tweakers
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    // Safety check - don't reprocess something we've already visited
                    if (allTweakerNames.contains(tweakName)) {
                        LOGGER.log(Level.WARN, "Tweak class name {} has already been visited -- skipping", tweakName);
                        // remove the tweaker from the stack otherwise it will create an infinite loop
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LOGGER.log(Level.INFO, "Loading tweak class name {}", tweakName);

                    // Ensure we allow the tweak class to load with the parent classloader
                    classLoader.addTransformerExclusion(tweakName.substring(0,tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).newInstance();
                    tweakers.add(tweaker);

                    // Remove the tweaker from the list of tweaker names we've processed this pass
                    it.remove();
                    // If we haven't visited a tweaker yet, the first will become the 'primary' tweaker
                    if (primaryTweaker == null) {
                        LOGGER.log(Level.INFO, "Using primary tweak class name {}", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                // Now, iterate all the tweakers we just instantiated
                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    LOGGER.log(Level.INFO, "Calling tweak class {}", tweaker.getClass().getName());
                    tweaker.acceptOptions(options.valuesOf(nonOption), minecraftHome, assetsDir, profileName);
                    tweaker.injectIntoClassLoader(classLoader);
                    allTweakers.add(tweaker);
                    // again, remove from the list once we've processed it, so we don't get duplicates
                    it.remove();
                }
                // continue around the loop until there's no tweak classes
            } while (!tweakClassNames.isEmpty());

            // Once we're done, we then ask all the tweakers for their arguments and add them all to the
            // master argument list
            for (final ITweaker tweaker : allTweakers) {
                argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }

            // Finally we turn to the primary tweaker, and let it tell us where to go to launch
            final String launchTarget = primaryTweaker.getLaunchTarget();
            final Class<?> clazz = Class.forName(launchTarget, false, classLoader);
            final Method mainMethod = clazz.getMethod("main", new Class[]{String[].class});

            LOGGER.info("Launching wrapped minecraft {%s}", launchTarget);
            mainMethod.invoke(null, (Object) argumentList.toArray(new String[0]));
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "Unable to launch", e);
            System.exit(1);
        }
    }
    
    private void fillTransformHandler(TransformHandler handler) {
        handler.runTransformersFunction = (name, transformedName, basicClass) -> {
            if (DEBUG_FINER) {
                LOGGER.debug("Beginning transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
                for (final IClassTransformer transformer : transformers.values()) {
                    final String transName = transformer.getClass().getName();
                    LOGGER.debug("Before Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                    basicClass = transformer.transform(name, transformedName, basicClass);
                    LOGGER.debug("After  Transformer {%s (%s)} %s: %d", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                }
                LOGGER.debug("Ending transform of {%s (%s)} Start Length: %d", name, transformedName, (basicClass == null ? 0 : basicClass.length));
            } else {
                for (final IClassTransformer transformer : transformers.values()) {
                    basicClass = transformer.transform(name, transformedName, basicClass);
                }
            }
            return basicClass;
        };
        handler.registerTransformerFunction = s -> {
            try {
                IClassTransformer transformer = (IClassTransformer) classLoader.loadClass(s).newInstance();
                transformers.put(s, transformer);
                if (transformer instanceof IClassNameTransformer && renameTransformer == null) {
                    renameTransformer = (IClassNameTransformer) transformer;
                }
            } catch (Exception e) {
                LOGGER.error("Error registering transformer class {}", s, e);
            }
        };
        handler.unRegisterTransformerFunction = s -> {
            try {
                transformers.remove(s);
            } catch (Exception e) {
                LOGGER.error("Error removing transformer class {}", s, e);
            }  
        };
        handler.runExplicitTransformersFunction = (name, basicClass) -> {
            TrieNode<Set<IExplicitTransformer>> node = explicitTransformers.getKeyValueNode(name);
            if (node != null) {
                Set<IExplicitTransformer> set = node.getValue();
                if (set != null) {
                    for (var transformer : set) {
                        basicClass = transformer.transform(name, basicClass);
                    }
                    set.clear(); // We are not doing hotswap, so classes only loaded once. Let's free their memory 
                }
            }
            return basicClass;
        };
        handler.registerExplicitTransformerFunction = (strings, s) -> {
            try {
                IExplicitTransformer instance = (IExplicitTransformer) classLoader.loadClass(s).newInstance();
                for (var target : strings) {
                    TrieNode<Set<IExplicitTransformer>> node =  explicitTransformers.getKeyValueNode(target);
                    if (node != null) {
                        node.getValue().add(instance);
                    } else {
                        var transformerSet = new TreeSet<IExplicitTransformer>();
                        transformerSet.add(instance);
                        explicitTransformers.put(target, transformerSet);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Error registering explicit transformer class {}", s, e);
            }
        };
        handler.transformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.remapClassName(s);
            }
            return s;
        };
        handler.unTransformNameFunction = s -> {
            if (renameTransformer != null) {
                return renameTransformer.unmapClassName(s);
            }
            return s;
        };
    }
}

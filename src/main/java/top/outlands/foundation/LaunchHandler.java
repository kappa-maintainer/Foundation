package top.outlands.foundation;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import top.outlands.foundation.transformer.ASMClassWriterTransformer;
import top.outlands.foundation.transformer.ASMVisitorTransformer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import static net.minecraft.launchwrapper.Launch.*;
import static top.outlands.foundation.TransformerDelegate.*;
import static top.outlands.foundation.boot.Foundation.LOGGER;

public class LaunchHandler {


    public void launch(String[] args) {

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
        Thread.currentThread().setContextClassLoader(classLoader);
        fillTransformerHolder(classLoader.getTransformerHolder());
        
        registerExplicitTransformerByInstance(new String[]{
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor",
        }, new ASMVisitorTransformer());
        registerExplicitTransformerByInstance(new String[]{
                "org.objectweb.asm.ClassWriter",
        }, new ASMClassWriterTransformer());
        try {
            classLoader.findClass("org.objectweb.asm.FieldVisitor");
            classLoader.findClass("org.objectweb.asm.ClassVisitor");
            classLoader.findClass("org.objectweb.asm.MethodVisitor");
            classLoader.findClass("org.objectweb.asm.ClassWriter");
        } catch (ClassNotFoundException e) {
            LOGGER.error("Can't find ASM", e);
        }
        classLoader.addTransformerExclusion("org.objectweb.asm.");

        blackboard.put("TweakClasses", tweakClassNames);
        blackboard.put("ArgumentList", argumentList);

        final Set<String> allTweakerNames = new HashSet<>();
        final List<ITweaker> allTweakers = new ArrayList<>();
        try {
            final List<ITweaker> tweakers = new ArrayList<>(tweakClassNames.size() + 1);
            blackboard.put("Tweaks", tweakers);
            ITweaker primaryTweaker = null;
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    if (allTweakerNames.contains(tweakName)) {
                        LOGGER.log(Level.WARN, "Tweak class name {} has already been visited -- skipping", tweakName);
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LOGGER.log(Level.INFO, "Loading tweak class name {}", tweakName);

                    classLoader.addTransformerExclusion(tweakName.substring(0,tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).getConstructor().newInstance();
                    tweakers.add(tweaker);

                    it.remove();
                    if (primaryTweaker == null) {
                        LOGGER.log(Level.INFO, "Using primary tweak class name {}", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    LOGGER.log(Level.INFO, "Calling tweak class {}", tweaker.toString());
                    tweaker.acceptOptions(options.valuesOf(nonOption), minecraftHome, assetsDir, profileName);
                    tweaker.injectIntoClassLoader(classLoader);
                    allTweakers.add(tweaker);
                    it.remove();
                }
            } while (!tweakClassNames.isEmpty());

            for (final ITweaker tweaker : allTweakers) {
                argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }

            final String launchTarget = primaryTweaker.getLaunchTarget();
            final Class<?> clazz = Class.forName(launchTarget, false, classLoader);
            final Method mainMethod = clazz.getMethod("main", String[].class);

            LOGGER.info("Launching wrapped minecraft {}", launchTarget);
            mainMethod.invoke(null, (Object) argumentList.toArray(new String[0]));
        } catch (Exception e) {
            LOGGER.fatal("Unable to launch", e);
            System.exit(1);
        }
    }
    

}

package top.outlands.foundation;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.MixinEnvironment;
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
        
        registerExplicitTransformerByInstance(
                new ASMVisitorTransformer(),
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor"
        );
        registerExplicitTransformerByInstance(
                new ASMClassWriterTransformer(),
                "org.objectweb.asm.ClassWriter"
        );
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
                        LOGGER.warn("Tweak name {} has already been visited -- skipping", tweakName);
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LOGGER.info("Loading tweak name {}", tweakName);

                    classLoader.addTransformerExclusion(tweakName.substring(0,tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).getConstructor().newInstance();
                    tweakers.add(tweaker);

                    it.remove();
                    if (primaryTweaker == null) {
                        LOGGER.info("Using primary tweak name {}", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                while (!tweakers.isEmpty()) {
                    final ITweaker tweaker = tweakers.getFirst();
                    LOGGER.info("Calling tweak {}", tweaker.toString());
                    tweaker.acceptOptions(options.valuesOf(nonOption), minecraftHome, assetsDir, profileName);
                    tweaker.injectIntoClassLoader(classLoader);
                    allTweakers.add(tweaker);
                    tweakers.remove(tweaker);
                }
            } while (!tweakClassNames.isEmpty());

            for (final ITweaker tweaker : allTweakers) {
                argumentList.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }
            MixinEnvironment.gotoPhase(MixinEnvironment.Phase.DEFAULT);

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

package top.outlands;


import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.outlands.foundation.boot.UnsafeHolder;
import top.outlands.foundation.trie.PrefixTrie;
import top.outlands.foundation.trie.TrieNode;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TrieTest {
    private final Logger log = (Logger) LogManager.getLogger("test");
    @Test
    public void TestGenericTrie() throws ClassNotFoundException {
        log.info("start testing");
        PrefixTrie<String> trie = new PrefixTrie<>();
        String[] keys = new String[]{"net.minecraft", "net.minecraftforge", "com.sun", "net.ibm", "net."};
        for (String key : keys) {
            log.info("putting " + key);
            trie.put(key, "first");
        }
        //Test key and value retrieving
        for (String key : keys) {
            log.info("getting " + key);
            log.info("node snippet is " + trie.getFirstKeyValueNode(key).getSnippet());
            Assertions.assertEquals(key, trie.getKeyValueNode(key).getKey());
            Assertions.assertEquals("first", trie.getKeyValueNode(key).getValue());
        }
        // Test value overwrite
        trie.put("net.ibm", "second");
        Assertions.assertEquals("second", trie.getKeyValueNode("net.ibm").getValue());
        // Test prefix matching
        Assertions.assertEquals("net.", trie.getFirstKeyValueNode("net.minecraft.client.FontRenderer").getKey());
        // Test all keys exist
        Assertions.assertEquals(Arrays.stream(keys).collect(Collectors.toSet()), trie.getRoot().getKeyValueNodes().stream().map(TrieNode::getKey).collect(Collectors.toSet()));
        trie = new PrefixTrie<>();
        trie.put("java.", "");
        trie.put("javax.", "");
        trie.put("jdk.", "");
        trie.put("sun.", "");
        trie.put("org.apache.", "");
        trie.put("org.burningwave.", "");
        trie.put("com.sun.", "");
        trie.put("net.minecraft.launchwrapper.LaunchClassLoader", "");
        trie.put("net.minecraft.launchwrapper.Launch", "");
        trie.put("top.outlands.foundation.boot.", "");
        trie.put("top.outlands.foundation.function.", "");
        trie.put("top.outlands.foundation.trie.", "");
        trie.put("io.github.toolfactory.jvm.", "");
        trie.put("org.burningwave.", "");
        trie.put("javassist", "");
        trie.put("com.google.", "");
        trie.put("com.cleanroommc.loader.", "");
        trie.put("net.minecraftforge.fml.relauncher.", "");
        trie.put("net.minecraftforge.classloading.", "");
        trie.put("net.minecraftforge.fml.common.asm.transformers.", "");
        trie.put("net.minecraftforge.fml.common.patcher.", "");
        trie.put("net.minecraftforge.fml.repackage.", "");
        trie.put("org.spongepowered.", "");
        trie.put("org.apache.commons.", "");
        trie.put("org.apache.http.", "");
        trie.put("org.apache.maven.", "");
        trie.put("com.google.common.", "");
        trie.put("org.objectweb.asm.", "");
        trie.put("LZMA.", "");
        trie.put("com.google.gson.", "");
        trie.put("com.google.common.", "");
        trie.put("com.google.thirdparty.publicsuffix.", "");
        log.info(trie.getRoot().getKeyValueNodes().stream().map(TrieNode::getKey).toList());
        log.info(trie.getFirstKeyValueNode("com.google.common.collect.RegularImmutableBiMap$Inverse$InverseEntrySet"));
        Assertions.assertNotNull(trie.getKeyValueNode("net.minecraftforge.fml.repackage."));

        trie = new PrefixTrie<>();
        keys = new String[] {
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor"
        };
        for (var s: keys) {
            trie.put(s, "");
        }
        for (var s: new String[]{"openmods.asm.", "openmods.include.", "openmods.core.", "openmods.injector.", "openmods.Log"}) {
            trie.put(s, "");
        }
        for (var s: keys) {
            Assertions.assertTrue(trie.getKeyValueNode(s).isKeyValueNode());
            Assertions.assertNotNull(trie.getNode(s));
        }
        trie.getRoot().getKeyValueNodes().forEach(node -> log.info(node.getKey()));
    }
    
}

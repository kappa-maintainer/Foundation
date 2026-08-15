package top.outlands;

import org.junit.jupiter.api.Test;
import top.outlands.foundation.trie.PrefixTrie;
import top.outlands.foundation.trie.TrieNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieTest {

    private static final String[] BASIC_KEYS = {"net.minecraft", "net.minecraftforge", "com.sun", "net.ibm", "net."};

    private static PrefixTrie<String> newBasicTrie() {
        PrefixTrie<String> trie = new PrefixTrie<>();
        for (String key : BASIC_KEYS) {
            assertTrue(trie.put(key, "first"), "put should accept " + key);
        }
        return trie;
    }

    @Test
    void putAndRetrieveExactKeys() {
        PrefixTrie<String> trie = newBasicTrie();

        for (String key : BASIC_KEYS) {
            TrieNode<String> node = trie.getKeyValueNode(key);
            assertNotNull(node, "exact key should be found: " + key);
            assertEquals(key, node.getKey());
            assertEquals("first", node.getValue());
        }
        assertEquals(BASIC_KEYS.length, trie.size());
    }

    @Test
    void putOverwritesExistingValue() {
        PrefixTrie<String> trie = new PrefixTrie<>();
        assertTrue(trie.put("net.ibm", "first"));
        assertTrue(trie.put("net.ibm", "second"));

        TrieNode<String> node = trie.getKeyValueNode("net.ibm");
        assertNotNull(node);
        assertEquals("second", node.getValue());
        assertEquals(1, trie.size(), "overwriting an existing key must not grow the trie");
    }

    @Test
    void getFirstKeyValueNodeReturnsShortestMatchingPrefix() {
        PrefixTrie<String> trie = newBasicTrie();

        // "net." is stored as its own key, so it shadows every longer "net.*" prefix
        assertEquals("net.", trie.getFirstKeyValueNode("net.minecraft.client.FontRenderer").getKey());
        assertEquals("net.", trie.getFirstKeyValueNode("net.minecraft").getKey());
        assertEquals("net.", trie.getFirstKeyValueNode("net.ibm").getKey());
        // no shorter prefix is stored for "com.sun"
        assertEquals("com.sun", trie.getFirstKeyValueNode("com.sun").getKey());
    }

    @Test
    void matchingIsBarePrefixWithoutPackageBoundary() {
        // bare-prefix semantics: "net.minecraft" also matches "net.minecraftforge.*"
        PrefixTrie<String> trie = new PrefixTrie<>();
        trie.put("net.minecraft", "");
        trie.put("net.minecraftforge", "");

        assertEquals("net.minecraft", trie.getFirstKeyValueNode("net.minecraftforge.extra.Class").getKey());
        assertNotNull(trie.getKeyValueNode("net.minecraftforge"));
        assertNull(trie.getKeyValueNode("net.minecraftforge.extra.Class"));
    }

    @Test
    void noMatchReturnsNull() {
        PrefixTrie<String> trie = newBasicTrie();

        assertNull(trie.getFirstKeyValueNode("no.such.prefix"));
        assertNull(trie.getFirstKeyValueNode(""));
        assertNull(trie.getKeyValueNode("net.minecraft.client.FontRenderer"));
    }

    @Test
    void unsupportedCharactersAreRejected() {
        PrefixTrie<String> trie = new PrefixTrie<>();
        assertFalse(trie.put("foo;bar", ""), "';' is not in the supported char set");
        assertFalse(trie.put("a/b", ""), "'/' is not in the supported char set");
        assertFalse(trie.put("has space", ""), "' ' is not in the supported char set");
        assertEquals(0, trie.size());
    }

    @Test
    void allStoredKeysAreRetrievableAsKeyValueNodes() {
        PrefixTrie<String> trie = newBasicTrie();
        Set<String> expected = new HashSet<>(Arrays.asList(BASIC_KEYS));
        Set<String> actual = trie.getRoot().getKeyValueNodes().stream()
                .map(TrieNode::getKey)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    @Test
    void sharedPrefixesSplitIntoDeepTrie() {
        PrefixTrie<String> trie = new PrefixTrie<>();
        String[] visitors = {
                "org.objectweb.asm.FieldVisitor",
                "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor"
        };
        for (String key : visitors) {
            assertTrue(trie.put(key, ""));
        }
        for (String key : new String[]{"openmods.asm.", "openmods.include.", "openmods.core.", "openmods.injector.", "openmods.Log"}) {
            assertTrue(trie.put(key, ""));
        }

        for (String key : visitors) {
            assertTrue(trie.getKeyValueNode(key).isKeyValueNode());
            assertNotNull(trie.getNode(key));
        }
    }

    @Test
    void classLoaderStyleExclusionList() {
        PrefixTrie<String> trie = new PrefixTrie<>();
        String[] keys = {
                "java.", "javax.", "jdk.", "sun.", "org.apache.", "org.burningwave.",
                "com.sun.", "net.minecraft.launchwrapper.LaunchClassLoader",
                "net.minecraft.launchwrapper.Launch", "top.outlands.foundation.boot.",
                "top.outlands.foundation.function.", "top.outlands.foundation.trie.",
                "io.github.toolfactory.jvm.", "javassist", "com.google.",
                "com.cleanroommc.loader.", "net.minecraftforge.fml.relauncher.",
                "net.minecraftforge.classloading.", "net.minecraftforge.fml.common.asm.transformers.",
                "net.minecraftforge.fml.common.patcher.", "net.minecraftforge.fml.repackage.",
                "org.spongepowered.", "org.apache.commons.", "org.apache.http.", "org.apache.maven.",
                "com.google.common.", "org.objectweb.asm.", "LZMA.", "com.google.gson.",
                "com.google.thirdparty.publicsuffix."
        };
        for (String key : keys) {
            assertTrue(trie.put(key, ""));
        }

        assertNotNull(trie.getKeyValueNode("net.minecraftforge.fml.repackage."));

        // both "com.google." and "com.google.common." are stored; the shortest prefix wins
        assertEquals("com.google.",
                trie.getFirstKeyValueNode("com.google.common.collect.RegularImmutableBiMap$Inverse$InverseEntrySet").getKey());
        assertEquals("org.spongepowered.",
                trie.getFirstKeyValueNode("org.spongepowered.asm.mixin.Mixin").getKey());
        assertEquals("org.objectweb.asm.",
                trie.getFirstKeyValueNode("org.objectweb.asm.ClassVisitor").getKey());
    }
}

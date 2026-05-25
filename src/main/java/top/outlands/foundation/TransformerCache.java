package top.outlands.foundation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Adler32;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.outlands.foundation.boot.TransformerHolder;

/**
 * Persistent cache of transformer output bytes, keyed by
 * (transformer class name, transformed class name, Adler32 + length of input bytes).
 *
 * <p>Avoids re-running expensive per-class transformer work (FML's
 * {@code DeobfuscationTransformer}, {@code SideTransformer},
 * {@code FieldRedirectTransformer}, {@code TerminalTransformer}) on warm
 * launches when the input bytecode hasn't changed.
 *
 * <p>Activated by the JVM system property {@code -Dfoundation.transformerCache=true}.
 * Off by default.
 *
 * <p>Persistence: dumped on shutdown hook to
 * {@code <gameDir>/foundation/transformerCache.bin} (or {@code java.io.tmpdir}
 * if {@code gameDir} is unset). Loaded on first install. Plain binary format
 * (no third-party deserialization to avoid gadget risk):
 * <pre>
 *   MAGIC (4 bytes ASCII "FTC1") + VERSION (byte = 1)
 *   COUNT (int)
 *   foreach: transformerName (UTF), className (UTF), inputAdler (long), inputLen (int),
 *            outputLen (int; -1 = "no change"), output (byte[])
 * </pre>
 *
 * <p>Adler32 + length is a non-cryptographic hash; collisions are theoretically
 * possible but vanishingly rare in practice (per-class bytecode is small and
 * unique). A collision produces silently wrong output bytes -- catastrophic if
 * it happens. The same caveat applies to VintageFix's prior art, which has
 * shipped on tens of thousands of MC 1.12 installs without reported collision
 * incidents.
 */
public final class TransformerCache {

    private static final Logger LOG = LogManager.getLogger("Foundation/TransformerCache");

    /** Public flag callers can read to skip overhead when cache is off. */
    public static final boolean ENABLED =
        Boolean.parseBoolean(System.getProperty("foundation.transformerCache", "false"));

    private static final int MAGIC = 0x46544331; // "FTC1"
    private static final byte VERSION = 1;

    /** Special marker stored in {@code output} field meaning "transformer was a no-op". */
    private static final byte[] NO_CHANGE = new byte[0];

    /**
     * Set of transformer class names we wrap. Mirrors VintageFix's allowlist.
     *
     * <p>IMPORTANT: do NOT add {@code org.spongepowered.asm.mixin.transformer.Proxy} or
     * any other Mixin-related transformer. Mixin's behaviour depends on the set of mixin
     * configs registered with {@code Mixins.addConfiguration}, which mods like FermiumBooter
     * and MixinBooter add LATE (during {@code Loader.loadMods}, after the first vanilla
     * classes have already been transformed by Proxy with the pre-late-config behaviour).
     * Caching Proxy's output would produce stale bytecode the second time the same class
     * is asked for through a different code path. The transformers listed here are
     * pure-function (output depends only on input bytes), so they're safe.
     */
    private static final java.util.Set<String> WRAP_CLASSES = new java.util.HashSet<>(java.util.Arrays.asList(
        "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer",
        "net.minecraftforge.fml.common.asm.transformers.SideTransformer",
        "net.minecraftforge.fml.common.asm.transformers.FieldRedirectTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TransformerLevelProperty"
    ));

    /** Cache map: (transformerName -> className -> entry). */
    private static final Map<String, Map<String, Entry>> CACHE = new ConcurrentHashMap<>();

    /** Set once disk-load + shutdown-hook have been done. */
    private static volatile boolean diskInitDone = false;

    /** Last observed size of TransformerHolder.transformers, to skip re-scanning. */
    private static volatile int lastListSize = -1;

    /** Track which transformer instances we've already wrapped. */
    private static final java.util.Set<IClassTransformer> wrappedInstances =
        java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Stats (best-effort, may slightly race). */
    private static long hits = 0, misses = 0, noChangeHits = 0, persistedEntries = 0;

    private TransformerCache() {}

    /** Returns true if {@code transformerClassName} should be wrapped by the cache. */
    public static boolean shouldWrap(String transformerClassName) {
        return WRAP_CLASSES.contains(transformerClassName);
    }

    /**
     * Look up a cache entry. Returns: byte[] output (which may be {@code NO_CHANGE} meaning
     * "transformer was a no-op, return input unchanged") or {@code null} on miss.
     */
    public static byte[] get(String transformerName, String className, byte[] input) {
        Map<String, Entry> bucket = CACHE.get(transformerName);
        if (bucket == null) return null;
        Entry e = bucket.get(className);
        if (e == null) return null;
        if (e.inputLen != input.length || e.inputAdler != adler(input)) {
            // Stale entry (input bytes changed); treat as miss + overwrite on the next put.
            return null;
        }
        if (e.output == NO_CHANGE) { noChangeHits++; }
        hits++;
        return e.output;
    }

    /** Record a transformer's (input, output) for future lookups. */
    public static void put(String transformerName, String className, byte[] input, byte[] output) {
        Entry e = new Entry();
        e.inputAdler = adler(input);
        e.inputLen = input.length;
        e.output = (output == null || java.util.Arrays.equals(input, output)) ? NO_CHANGE : output;
        CACHE.computeIfAbsent(transformerName, k -> new ConcurrentHashMap<>()).put(className, e);
        misses++;
    }

    /**
     * Install caching proxies around all cacheable transformers in the current
     * {@code TransformerHolder.transformers} list. Idempotent and re-entrant safe.
     *
     * <p>Called repeatedly from {@code runTransformersFunction} because transformers
     * are registered incrementally by the tweaker chain (FMLDeobfTweaker registers
     * DeobfuscationTransformer late, after class loading has already begun for some
     * pre-mod classes). We keep retrying until every WRAP_CLASSES entry has been
     * observed and wrapped, then short-circuit via the {@code installed} flag.
     *
     * <p>Returns the number of transformers wrapped on this call (0 if nothing new).
     */
    public static int installProxies() {
        // Fast path: list size unchanged since last scan → nothing new to wrap.
        java.util.List<IClassTransformer> list = TransformerHolder.transformers;
        if (list == null) return 0;
        int size = list.size();
        if (size == lastListSize) return 0;
        return installProxiesSync(list, size);
    }

    private static synchronized int installProxiesSync(java.util.List<IClassTransformer> list, int observedSize) {
        // Re-check under the lock in case another thread already handled it.
        if (observedSize == lastListSize) return 0;
        if (!diskInitDone) {
            loadFromDisk();
            Runtime.getRuntime().addShutdownHook(new Thread(TransformerCache::saveToDisk, "TransformerCacheSave"));
            diskInitDone = true;
        }
        int wrapped = 0;
        for (int i = 0; i < list.size(); i++) {
            IClassTransformer t = list.get(i);
            if (t instanceof CachedTransformerProxy) continue;
            String name = t.getClass().getName();
            if (!shouldWrap(name)) continue;
            if (wrappedInstances.contains(t)) continue;
            IClassTransformer proxy;
            if (t instanceof IClassNameTransformer) {
                proxy = new CachedNameTransformerProxy((IClassNameTransformer & IClassTransformer) t);
            } else {
                proxy = new CachedTransformerProxy(t);
            }
            list.set(i, proxy);
            wrappedInstances.add(t);
            LOG.info("Installed cache proxy for {}", name);
            wrapped++;
        }
        lastListSize = list.size();
        return wrapped;
    }

    private static long adler(byte[] data) {
        Adler32 a = new Adler32();
        a.update(data);
        return a.getValue();
    }

    /** Cache file location. */
    private static File cacheFile() {
        String gameDir = System.getProperty("user.dir");
        File dir = new File(gameDir, "foundation");
        dir.mkdirs();
        return new File(dir, "transformerCache.bin");
    }

    private static void loadFromDisk() {
        File f = cacheFile();
        if (!f.isFile()) {
            LOG.info("No transformer cache on disk at {}; starting fresh", f);
            return;
        }
        long t0 = System.nanoTime();
        int loaded = 0;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            int magic = in.readInt();
            byte version = in.readByte();
            if (magic != MAGIC || version != VERSION) {
                LOG.warn("Transformer cache header mismatch (magic={}, version={}); discarding", Integer.toHexString(magic), version);
                return;
            }
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                String tn = in.readUTF();
                String cn = in.readUTF();
                long ha = in.readLong();
                int il = in.readInt();
                int ol = in.readInt();
                byte[] out;
                if (ol < 0) {
                    out = NO_CHANGE;
                } else {
                    out = new byte[ol];
                    in.readFully(out);
                }
                if (!shouldWrap(tn)) continue; // transformer no longer cacheable
                Entry e = new Entry();
                e.inputAdler = ha;
                e.inputLen = il;
                e.output = out;
                CACHE.computeIfAbsent(tn, k -> new ConcurrentHashMap<>()).put(cn, e);
                loaded++;
            }
            persistedEntries = loaded;
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            LOG.info("Loaded transformer cache: {} entries in {} ms", loaded, ms);
        } catch (IOException e) {
            LOG.warn("Failed to load transformer cache; discarding", e);
            CACHE.clear();
        }
    }

    private static void saveToDisk() {
        File f = cacheFile();
        long t0 = System.nanoTime();
        int saved = 0;
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            // Count first by summing inner-map sizes.
            int count = 0;
            for (Map<String, Entry> m : CACHE.values()) count += m.size();
            out.writeInt(count);
            for (Map.Entry<String, Map<String, Entry>> outer : CACHE.entrySet()) {
                String tn = outer.getKey();
                for (Map.Entry<String, Entry> inner : outer.getValue().entrySet()) {
                    Entry e = inner.getValue();
                    out.writeUTF(tn);
                    out.writeUTF(inner.getKey());
                    out.writeLong(e.inputAdler);
                    out.writeInt(e.inputLen);
                    if (e.output == NO_CHANGE) {
                        out.writeInt(-1);
                    } else {
                        out.writeInt(e.output.length);
                        out.write(e.output);
                    }
                    saved++;
                }
            }
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            LOG.info("Saved transformer cache: {} entries (hits={} misses={} noChangeHits={}) in {} ms",
                saved, hits, misses, noChangeHits, ms);
        } catch (IOException e) {
            LOG.warn("Failed to save transformer cache", e);
        }
    }

    /** Cache entry. */
    private static final class Entry {
        long inputAdler;
        int inputLen;
        byte[] output;
    }
}

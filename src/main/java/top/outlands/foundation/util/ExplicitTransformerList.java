package top.outlands.foundation.util;

import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.Supplier;

import top.outlands.foundation.boot.Foundation;
import top.outlands.foundation.function.transformer.IExplicitTransformer;

public class ExplicitTransformerList<T extends IExplicitTransformer>{
    private final Map<String, PriorityQueue<T>> transformers;
    private final Supplier<Logger> logger;

    public ExplicitTransformerList(){
        this(() -> new HashMap<>(20), () -> Foundation.LOGGER);
    }

    public ExplicitTransformerList(Supplier<Map<String, PriorityQueue<T>>> constructor, Supplier<Logger> logger){
        this.transformers = constructor.get();
        this.logger = logger;
    }

    public void register(T transformer, String... targets){
        if (targets.length == 0) return;
        logger.get().debug("Registering explicit transformer instance: {}", transformer.getClass().getSimpleName());
        try {
            for (String target : targets) {
                if (transformers.containsKey(target)) {
                    transformers.get(target).add(transformer);
                } else {
                    PriorityQueue<T> transformerSet = new PriorityQueue<>(Comparator.comparingInt(IExplicitTransformer::getPriority));
                    transformerSet.add(transformer);
                    transformers.put(target, transformerSet);
                }
            }

        } catch (Exception e) {
            logger.get().error("Error registering explicit transformer class {}", transformer.getClass().getSimpleName(), e);
        }
    }

    public PriorityQueue<T> getTransformer(String transformedName) {
        return transformers.get(transformedName);
    }

    public T run(String transformedName, T value) {
        PriorityQueue<T> queue = transformers.get(transformedName);
        if (queue != null) {
            while (!queue.isEmpty()) {
                value = queue.poll().transform(value); // We are not doing hotswap, so classes only loaded once. Let's free their memory
            }
            transformers.remove(transformedName); // GC
        }
        return T;
    }
}

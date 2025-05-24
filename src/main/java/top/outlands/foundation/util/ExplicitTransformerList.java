package top.outlands.foundation.util;

import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;

import top.outlands.foundation.function.transformer.IExplicitTransformer;

public class ExplicitTransformerList<T>{
    private final Map<String, PriorityQueue<IExplicitTransformer<T>>> transformers;
    private final Logger logger;

    public ExplicitTransformerList(Map<String, PriorityQueue<IExplicitTransformer<T>>> map, Logger logger){
        this.transformers = map;
        this.logger = logger;
    }

    public void register(IExplicitTransformer<T> transformer, String... targets){
        if (targets.length == 0) return;
        logger.debug("Registering explicit transformer instance: {}", transformer.getClass().getSimpleName());
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

    public PriorityQueue<IExplicitTransformer<T>> getTransformer(String transformedName) {
        return transformers.get(transformedName);
    }

    public Map<String, PriorityQueue<IExplicitTransformer<T>>> getTransformers() {
        return transformers;
    }

    public T run(String transformedName, T value) {
        PriorityQueue<IExplicitTransformer<T>> queue = transformers.get(transformedName);
        if (queue != null) {
            while (!queue.isEmpty()) {
                value = queue.poll().transform(value); // We are not doing hotswap, so classes only loaded once. Let's free their memory
            }
            transformers.remove(transformedName); // GC
        }
        return T;
    }
}

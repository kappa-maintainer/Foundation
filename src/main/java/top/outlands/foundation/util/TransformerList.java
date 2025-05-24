package top.outlands.foundation.util;

import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.function.Predicate;

import top.outlands.foundation.function.transformer.ITransformer;

public class TransformerList<T> {
    private final List<ITransformer<T>> transformers;
    private final Logger logger;

    public ExplicitTransformerList(ITransformer<T> list, Logger logger){
        this.transformers = list;
        this.logger = logger;
    }

    public List<ITransformer<T>> getTransformers() {
        returm transformers;
    }

    public void register(ITransformer<T> transformer){
        logger.debug("Registering transformer instance: {}", transformer.getClass().getName());
        transformers.add(transformer);
        transformers.sort(Comparator.comparingInt(ITransformer::getPriority));
    }

    public void unregister(ITransformer<T> transformer) {
        logger.debug("Unregistering transformer: {}", transformer.getClass());
        try {
            transformers.remove(transformer);
            transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", transformer, e);
        }
    }

    public void unregister(Predicate<ITransformer<T>> predicate) {
        try {
            Iterator<ITransformer<T>> itr = transformers.iterator();
            while (itr.hasNext()) {
                if (predicate.test(itr.next())) {
                    itr.remove();
                } 
            }
            transformers.sort(Comparator.comparingInt(IClassTransformer::getPriority));
        } catch (Exception e) {
            LOGGER.error("Error removing transformer class {}", transformer, e);
        }
    }

    public T run(String name, String transformedName, T value) {
        for(T transformer : Collections.unmodifiableList(transformers)) {
            vlaue = transformer.transform(name, transformedName, value);
        }
        return T;
    }
}

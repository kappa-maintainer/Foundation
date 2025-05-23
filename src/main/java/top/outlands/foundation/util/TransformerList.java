package top.outlands.foundation.util;

import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.function.Supplier;

import top.outlands.foundation.boot.Foundation;
import top.outlands.foundation.function.transformer.ITransformer;

public class TransformerList<T extends ITransformer> {
    private final List<T> transformers;
    private final Supplier<Logger> logger;

    public ExplicitTransformerList(){
        this(LinkedList::new, () -> Foundation.LOGGER);
    }

    public ExplicitTransformerList(Supplier<List<T>> constructor, Supplier<Logger> logger){
        this.transformers = constructor.get();
        this.logger = logger;
    }

    public List<T> getTransformers() {
        returm transformers;
    }

    public void register(T transformer){
        logger.get().debug("Registering transformer instance: {}", transformer.getClass().getName());
        transformers.add(transformer);
    }

    public T run(String name, String transformedName, T value) {
        for(T transformer : transformers) {
            vlaue = transformer.transform(name, transformedName, value);
        }
        return T;
    }
}

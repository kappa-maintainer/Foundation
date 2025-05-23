package net.minecraft.launchwrapper;

import top.outlands.foundation.function.transformer.ITransformer;

/**
 * Good old transformer interface. Only use this for global transforming; if you know what your targets are, use {@link top.outlands.foundation.IExplicitTransformer}.
 */
public interface IClassTransformer extends ITransformer<byte[]>{
}

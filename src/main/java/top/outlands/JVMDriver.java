package top.outlands;

import io.github.toolfactory.jvm.Driver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class JVMDriver {
    /**
     * The JVM Driver, native ver.
     */
    public static Driver DRIVER = Driver.Factory.getNewNative().init();
    public static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Optional<Field> f = Arrays.stream(DRIVER.getDeclaredFields(clazz)).filter(field -> field.getName().equals(name)).findFirst();
        if (f.isPresent()) {
            return f.get();
        } else {
            throw new NoSuchFieldException(name);
        }
    }

    /**
     * Will only fetch the first matched method
     * @param clazz Target class
     * @param name Target method name
     * @return The first method match the name
     * @throws NoSuchMethodException
     */
    public static Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        
        Optional<Method> m = Arrays.stream(DRIVER.getDeclaredMethods(clazz)).filter(method -> method.getName().equals(name)).findFirst();
        if (m.isPresent()) {
            return m.get();
        } else {
            throw new NoSuchMethodException(name);
        }
    }

    public static Method findMethodExplicitly(Class<?> clazz, String name, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Optional<Method> m = Arrays.stream(DRIVER.getDeclaredMethods(clazz)).filter(method -> method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameterTypes)).findFirst();
        if (m.isPresent()) {
            return m.get();
        } else {
            throw new NoSuchMethodException(name);
        }
    }
    
    public static Constructor<?> findConstructorExplicitly(Class<?> clazz, Class<?>... parameterTypes) throws NoSuchMethodException {
        Optional<? extends Constructor<?>> c = Arrays.stream(DRIVER.getDeclaredConstructors(clazz)).filter(constructor -> Arrays.equals(constructor.getParameterTypes(), parameterTypes)).findFirst();
        if (c.isPresent()) {
            return c.get();
        } else {
            throw new NoSuchMethodException("Couldn't find" + clazz.getSimpleName() + "'s specified constructor");
        }
    }
}

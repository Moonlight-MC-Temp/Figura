package org.figuramc.figura.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SpigotUtils {
    public static Object call(Object o, String methodName, Class<?>[] types, Object... args) {
        try {
            Class<?> clazz = o.getClass();
            Method m = clazz.getDeclaredMethod(methodName, types);
            return m.invoke(o, args);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

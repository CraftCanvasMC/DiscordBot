package io.canvasmc.discord.models;

import dev.jsinco.discord.framework.logging.FrameWorkLogger;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class FieldRegistry<T extends FieldRegistry<T>> {

    private static final Map<Class<?>, Map<String, ?>> REGISTRY = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> registryFor(Class<T> clazz) {
        return (Map<String, T>) REGISTRY.computeIfAbsent(clazz, k -> {
            Map<String, T> map = new LinkedHashMap<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (clazz.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        map.put(field.getName(), (T) field.get(null));
                    } catch (IllegalAccessException e) {
                        FrameWorkLogger.error("Failed to access field " + field.getName(), e);
                    }
                }
            }
            return map;
        });
    }

    public static <T extends FieldRegistry<T>> T get(Class<T> clazz, String key) {
        return registryFor(clazz).get(key);
    }

    public static <T extends FieldRegistry<T>> Set<String> keys(Class<T> clazz) {
        return registryFor(clazz).keySet();
    }

    public static <T extends FieldRegistry<T>> Collection<T> values(Class<T> clazz) {
        return registryFor(clazz).values();
    }
}
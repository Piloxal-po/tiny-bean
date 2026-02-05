package com.github.oxal.injector;

import com.github.oxal.utils.PropertyLoader;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

@Slf4j
public class ConfigurationInjector {

    public static void inject(Object instance, String prefix) {
        if (instance == null) {
            return;
        }

        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String propertyKey = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();

            if (isSimpleType(field.getType())) {
                String value = PropertyLoader.getProperty(propertyKey);
                if (value != null) {
                    try {
                        Object convertedValue = convert(value, field.getType());
                        field.set(instance, convertedValue);
                        log.debug("Injected property '{}' with value '{}' into field '{}'", propertyKey, value, field.getName());
                    } catch (Exception e) {
                        log.error("Failed to inject property '{}' into field '{}'", propertyKey, field.getName(), e);
                    }
                }
            } else {
                // Recursive injection for nested objects
                try {
                    Object nestedInstance = field.get(instance);
                    if (nestedInstance == null) {
                        try {
                            nestedInstance = field.getType().getDeclaredConstructor().newInstance();
                            field.set(instance, nestedInstance);
                        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                            log.warn("Could not instantiate nested configuration object for field '{}'. Skipping recursive injection.", field.getName());
                            continue;
                        }
                    }
                    inject(nestedInstance, propertyKey);
                } catch (IllegalAccessException e) {
                    log.error("Failed to access field '{}' for recursive injection", field.getName(), e);
                }
            }
        }
    }

    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() ||
                type.equals(String.class) ||
                type.equals(Integer.class) ||
                type.equals(Long.class) ||
                type.equals(Double.class) ||
                type.equals(Float.class) ||
                type.equals(Boolean.class) ||
                type.equals(Byte.class) ||
                type.equals(Short.class) ||
                type.equals(Character.class);
    }

    private static Object convert(String value, Class<?> targetType) {
        if (targetType.equals(String.class)) {
            return value;
        }
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        if (targetType.equals(long.class) || targetType.equals(Long.class)) {
            return Long.parseLong(value);
        }
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            return Double.parseDouble(value);
        }
        if (targetType.equals(float.class) || targetType.equals(Float.class)) {
            return Float.parseFloat(value);
        }
        throw new IllegalArgumentException("Unsupported type for conversion: " + targetType.getName());
    }
}

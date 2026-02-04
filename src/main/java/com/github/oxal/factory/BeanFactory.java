package com.github.oxal.factory;

import com.github.oxal.runner.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Slf4j
public class BeanFactory {

    public <T> T createBeanInstance(Executable executable) {
        log.trace("createBeanInstance called for executable: {}", executable);
        if (executable instanceof Method method) {
            return loadBeanByMethod(method);
        } else if (executable instanceof Constructor<?> constructor) {
            return (T) loadBeanByConstructor(constructor);
        }
        log.error("Unsupported executable type: {}", executable.getClass().getName());
        throw new IllegalStateException("Unsupported executable type: " + executable.getClass().getName());
    }

    private <T> T processLoad(Executable executable, BiFunction<Object, Object[], T> invocation) {
        Class<?>[] paramTypes = executable.getParameterTypes();
        log.debug("Resolving {} dependencies for '{}': {}", paramTypes.length, executable.getName(), Arrays.stream(paramTypes).map(Class::getSimpleName).collect(Collectors.joining(", ")));

        Class<?> classContainer = executable.getDeclaringClass();
        Object instance = null;

        if (executable instanceof Method) {
            try {
                Constructor<?> c = classContainer.getDeclaredConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            } catch (NoSuchMethodException e) {
                log.error("Class {} must have a no-arg constructor to host @Bean methods.", classContainer.getSimpleName(), e);
                throw new RuntimeException(classContainer.getSimpleName() + " must have a no-arg constructor to host @Bean methods.", e);
            } catch (Exception e) {
                log.error("Failed to instantiate configuration class {}", classContainer.getSimpleName(), e);
                throw new RuntimeException("Failed to instantiate configuration class " + classContainer.getSimpleName(), e);
            }
        }

        try {
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                com.github.oxal.annotation.Qualifier qualifier = executable.getParameters()[i].getAnnotation(com.github.oxal.annotation.Qualifier.class);
                String qualifierName = (qualifier != null) ? qualifier.value() : null;
                log.trace("Loading dependency #{}: type={}, qualifier='{}'", i, paramTypes[i].getName(), qualifierName);
                args[i] = ApplicationRunner.loadBean(paramTypes[i], qualifierName);
            }
            log.debug("All dependencies resolved. Invoking executable...");
            return invocation.apply(instance, args);
        } catch (Exception e) {
            log.error("Failed to resolve dependencies for {}", executable.getDeclaringClass().getName(), e);
            throw new RuntimeException("Failed to resolve dependencies for " + executable.getDeclaringClass().getName(), e);
        }
    }

    private <T> T loadBeanByMethod(Method method) {
        return processLoad(method, (instance, args) -> {
            try {
                method.setAccessible(true);
                T bean = (T) method.invoke(instance, args);
                log.trace("Successfully invoked @Bean method: {}", method.getName());
                return bean;
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke @Bean method: {}", method.getName(), e);
                throw new RuntimeException("Failed to invoke @Bean method: " + method.getName(), e);
            }
        });
    }

    private <T> T loadBeanByConstructor(Constructor<T> constructor) {
        return processLoad(constructor, (instance, args) -> {
            try {
                constructor.setAccessible(true);
                T bean = constructor.newInstance(args);
                log.trace("Successfully invoked constructor: {}", constructor.getName());
                return bean;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke constructor: {}", constructor.getName(), e);
                throw new RuntimeException("Failed to invoke constructor: " + constructor.getName(), e);
            }
        });
    }
}

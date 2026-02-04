package com.github.oxal.factory;

import com.github.oxal.runner.ApplicationRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class BeanFactory {

    public <T> T createBeanInstance(Executable executable) {
        if (executable instanceof Method method) {
            return loadBeanByMethod(method);
        } else if (executable instanceof Constructor<?> constructor) {
            return (T) loadBeanByConstructor(constructor);
        }
        throw new IllegalStateException("Unsupported executable type: " + executable.getClass().getName());
    }

    private <T> T processLoad(Executable executable, BiFunction<Object, Object[], T> invocation) {
        Class<?> classContainer = executable.getDeclaringClass();
        Object instance = null;

        if (executable instanceof Method) {
            try {
                Constructor<?> c = classContainer.getDeclaredConstructor();
                c.setAccessible(true);
                instance = c.newInstance();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(classContainer.getSimpleName() + " must have a no-arg constructor to host @Bean methods.", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate configuration class " + classContainer.getSimpleName(), e);
            }
        }

        try {
            Object[] args = new Object[executable.getParameterCount()];
            for (int i = 0; i < executable.getParameterCount(); i++) {
                com.github.oxal.annotation.Qualifier qualifier = executable.getParameters()[i].getAnnotation(com.github.oxal.annotation.Qualifier.class);
                String qualifierName = (qualifier != null) ? qualifier.value() : null;
                args[i] = ApplicationRunner.loadBean(executable.getParameterTypes()[i], qualifierName);
            }
            return invocation.apply(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve dependencies for " + executable.getDeclaringClass().getName(), e);
        }
    }

    private <T> T loadBeanByMethod(Method method) {
        return processLoad(method, (instance, args) -> {
            try {
                method.setAccessible(true);
                return (T) method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke @Bean method: " + method.getName(), e);
            }
        });
    }

    private <T> T loadBeanByConstructor(Constructor<T> constructor) {
        return processLoad(constructor, (instance, args) -> {
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke @Bean constructor: " + constructor.getName(), e);
            }
        });
    }
}

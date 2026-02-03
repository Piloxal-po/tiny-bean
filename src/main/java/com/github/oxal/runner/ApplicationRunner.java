package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;
import com.github.oxal.annotation.ScopeType;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.scanner.ApplicationScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class ApplicationRunner {

    public static void loadContext(Class<?> application) {
        if (!application.isAnnotationPresent(Application.class)) {
            throw new RuntimeException(Application.class.getName() + " is not present in annotation of " + application.getName());
        }

        Application configuration = application.getAnnotation(Application.class);

        Set<String> packagesToScan = new HashSet<>();
        packagesToScan.add(application.getPackageName());
        packagesToScan.addAll(Arrays.asList(configuration.packages()));

        String[] packages = packagesToScan.toArray(new String[0]);

        Context context = ContextService.createContexte(application, packages);
        ApplicationScanner.scanBeans(packages);
        System.out.println("Bean definitions found: " + context.getBeanDefinitions().size());
    }

    public static <T> T loadBean(Class<T> beanClass) {
        return loadBean(beanClass, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadBean(Class<T> beanClass, String beanName) {
        if (beanClass == null) {
            throw new IllegalArgumentException("beanClass cannot be null");
        }

        Context context = ContextService.getContext();
        KeyDefinition key = context.getBeanDefinitionKey(beanClass, beanName)
                .orElseThrow(() -> new RuntimeException("No bean definition found for class: " + beanClass.getName() + " and name: " + beanName));

        Executable executable = context.getBeanDefinitions().get(key);
        Bean beanAnnotation = executable.getAnnotation(Bean.class);
        if (beanAnnotation == null) { // Should not happen if scanner is correct
            beanAnnotation = executable.getDeclaringClass().getAnnotation(Bean.class);
        }

        if (beanAnnotation.scope() == ScopeType.PROTOTYPE) {
            return createBeanInstance(executable);
        }

        // Default to SINGLETON
        return (T) context.getSingletonInstances().computeIfAbsent(key, k -> createBeanInstance(executable));
    }

    private static <T> T createBeanInstance(Executable executable) {
        if (executable instanceof Method method) {
            return loadBeanByMethod(method);
        } else if (executable instanceof Constructor<?> constructor) {
            return (T) loadBeanByConstructor(constructor);
        }
        throw new IllegalStateException("Unsupported executable type: " + executable.getClass().getName());
    }

    private static <T> T processLoad(Executable executable, BiFunction<Object, Object[], T> invocation) {
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
                Qualifier qualifier = executable.getParameters()[i].getAnnotation(Qualifier.class);
                String qualifierName = (qualifier != null) ? qualifier.value() : null;
                args[i] = loadBean(executable.getParameterTypes()[i], qualifierName);
            }
            return invocation.apply(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve dependencies for " + executable.getName(), e);
        }
    }

    private static <T> T loadBeanByMethod(Method method) {
        return processLoad(method, (instance, args) -> {
            try {
                method.setAccessible(true);
                return (T) method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke @Bean method: " + method.getName(), e);
            }
        });
    }

    private static <T> T loadBeanByConstructor(Constructor<T> constructor) {
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

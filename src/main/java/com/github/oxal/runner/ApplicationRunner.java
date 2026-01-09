package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.annotation.Qualifier;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.scanner.ApplicationScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class ApplicationRunner {

    public static void loadContext(Class<?> application) {
        if (!application.isAnnotationPresent(Application.class)) {
            throw new RuntimeException(Application.class.getName() + " is not present in annotation of " + application.getName());
        }

        Application Configuration = application.getAnnotation(Application.class);

        String[] packages = Configuration.packages();
        if (Configuration.packages().length == 0) {
            packages = new String[]{application.getPackageName()};
        }

        Context contexte = ContextService.createContexte(application, packages);

        ApplicationScanner.scanBeans(packages);

        System.out.println("Beans found: " + contexte.getBeans().size());
    }

    public static <T> T loadBean(Class<T> beanClass) {
        return loadBean(beanClass, null);
    }

    public static <T> T loadBean(Class<T> beanClass, String beanName) {
        if (beanClass == null) {
            throw new RuntimeException("beanClass is null");
        }

        T t = null;

        try {
            return ContextService.getContext().getExecutableComputed(beanClass, beanName);
        } catch (Exception e) {
            Executable executable = ContextService.getContext().getExecutable(beanClass, beanName);
            if (executable instanceof Method method) {
                t = loadBeanByMethod(method);
            } else if (executable instanceof Constructor constructor) {
                t = (T) loadBeanByConstructor(constructor);
            }
        }

        if (t == null) {
            throw new RuntimeException("beanClass: " + beanClass.getName() + " is not a bean");
        }

        ContextService.getContext().addBeanComputed(beanClass, beanName, t);
        ContextService.getContext().deleteBean(beanClass, beanName);
        return t;
    }

    private static <T> T processLoad(Executable executable, BiFunction<Object, Object[], T> invocation) {
        Class<?> classContainer = executable.getDeclaringClass();
        Object instance = null;
        try {
            Constructor c = classContainer.getDeclaredConstructor();
            instance = c.newInstance();
        } catch (NoSuchMethodException e) {
            if (executable instanceof Method) {
                throw new RuntimeException(classContainer.getSimpleName()  + " have not an empty constructor");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            Object[] args = new Object[executable.getParameterCount()];
            for (int i = 0; i < executable.getParameterCount(); i++) {
                Qualifier qualifier = null;
                try {
                    qualifier = executable.getParameters()[i].getAnnotation(Qualifier.class);
                } catch (Exception ignored) {
                }
                args[i] = loadBean(executable.getParameterTypes()[i], qualifier != null ? qualifier.value() : null);
            }
            return invocation.apply(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T loadBeanByMethod(Method method) {
        return processLoad(method, (instance, args) -> {
            try {
                return (T) method.invoke(instance, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <T> T loadBeanByConstructor(Constructor<T> constructor) {
        return processLoad(constructor, (instance, args) -> {
            try {
                return constructor.newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

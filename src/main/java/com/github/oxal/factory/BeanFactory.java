package com.github.oxal.factory;

import com.github.oxal.annotation.Configuration;
import com.github.oxal.injector.ConfigurationInjector;
import com.github.oxal.runner.ApplicationRunner;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

@Slf4j
public class BeanFactory {

    public static <T> T loadBean(Class<T> beanClass, String beanName) {
        return ApplicationRunner.loadBean(beanClass, beanName);
    }

    public static <T> T loadBean(Class<T> beanClass) {
        return ApplicationRunner.loadBean(beanClass);
    }

    public static <T> T createBeanInstance(Executable executable) {
        log.trace("createBeanInstance called for executable: {}", executable);
        if (executable instanceof Method method) {
            return loadBeanByMethod(method);
        } else if (executable instanceof Constructor<?> constructor) {
            return (T) loadBeanByConstructor(constructor);
        }
        log.error("Unsupported executable type: {}", executable.getClass().getName());
        throw new IllegalStateException("Unsupported executable type: " + executable.getClass().getName());
    }

    private static <T> T processLoad(Executable executable, BiFunction<Object, Object[], T> invocation) {
        log.debug("Resolving dependencies for '{}'", executable.getName());

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
            Object[] args = resolveArguments(executable);
            log.debug("All dependencies resolved. Invoking executable...");
            return invocation.apply(instance, args);
        } catch (Exception e) {
            log.error("Failed to resolve dependencies for {}", executable.getDeclaringClass().getName(), e);
            throw new RuntimeException("Failed to resolve dependencies for " + executable.getDeclaringClass().getName(), e);
        }
    }

    private static Object[] resolveArguments(Executable executable) {
        Parameter[] parameters = executable.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            com.github.oxal.annotation.Qualifier qualifier = parameter.getAnnotation(com.github.oxal.annotation.Qualifier.class);
            String qualifierName = (qualifier != null) ? qualifier.value() : null;

            if (List.class.isAssignableFrom(parameter.getType())) {
                args[i] = loadBeanByParameter(parameter, i);
            } else if (Set.class.isAssignableFrom(parameter.getType())) {
                List<?> list = loadBeanByParameter(parameter, i);
                Set<?> set = new HashSet<>(list == null ? List.of() : list);
                args[i] = set;
            } else {
                // Standard single bean injection
                log.trace("Loading dependency #{}: type={}, qualifier='{}'", i, parameter.getType().getName(), qualifierName);
                args[i] = ApplicationRunner.loadBean(parameter.getType(), qualifierName);
            }
        }
        return args;
    }

    private static List<?> loadBeanByParameter(Parameter parameter, int i) {
        Type genericType = parameter.getParameterizedType();
        if (genericType instanceof ParameterizedType) {
            Type actualTypeArgument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (actualTypeArgument instanceof Class) {
                Class<?> beanType = (Class<?>) actualTypeArgument;
                log.trace("Loading list of beans for dependency #{}: List<{}>", i, beanType.getSimpleName());
                return ApplicationRunner.loadBeans(beanType);
            }
            log.warn("Complex generic type for List injection not fully supported: {}", actualTypeArgument);
        } else {
            log.warn("List injection without generic type is not supported. Use List<Type>.");
        }

        return null;
    }

    private static <T> T loadBeanByMethod(Method method) {
        return processLoad(method, (instance, args) -> {
            try {
                method.setAccessible(true);
                T bean = (T) method.invoke(instance, args);
                log.trace("Successfully invoked @Bean method: {}", method.getName());
                injectConfigurationProperties(bean);
                return bean;
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke @Bean method: {}", method.getName(), e);
                throw new RuntimeException("Failed to invoke @Bean method: " + method.getName(), e);
            }
        });
    }

    private static <T> T loadBeanByConstructor(Constructor<T> constructor) {
        return processLoad(constructor, (instance, args) -> {
            try {
                constructor.setAccessible(true);
                T bean = constructor.newInstance(args);
                log.trace("Successfully invoked constructor: {}", constructor.getName());
                injectConfigurationProperties(bean);
                return bean;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to invoke constructor: {}", constructor.getName(), e);
                throw new RuntimeException("Failed to invoke constructor: " + constructor.getName(), e);
            }
        });
    }

    private static void injectConfigurationProperties(Object bean) {
        if (bean == null) return;
        Class<?> beanClass = bean.getClass();
        if (beanClass.isAnnotationPresent(Configuration.class)) {
            Configuration config = beanClass.getAnnotation(Configuration.class);
            String prefix = config.prefix();
            log.debug("Injecting configuration properties for bean '{}' with prefix '{}'", beanClass.getSimpleName(), prefix);
            ConfigurationInjector.inject(bean, prefix);
        }
    }
}

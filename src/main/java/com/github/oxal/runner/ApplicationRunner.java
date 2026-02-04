package com.github.oxal.runner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.factory.BeanFactory;
import com.github.oxal.initializer.ContextInitializer;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.resolver.BeanDefinitionResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

public class ApplicationRunner {

    private static final BeanFactory beanFactory = new BeanFactory();
    private static final BeanDefinitionResolver beanDefinitionResolver = new BeanDefinitionResolver();
    private static final ContextInitializer contextInitializer = new ContextInitializer();

    public static void loadContext(Class<?> application) {
        contextInitializer.initialize(application);
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
        if (beanClass.equals(Context.class)) {
            return (T) context;
        }

        KeyDefinition key = beanDefinitionResolver.resolve(beanClass, beanName, context);

        if (context.isBeanInCreation(key)) {
            throw new RuntimeException("Circular dependency detected for bean: " + key);
        }

        Executable executable = context.getBeanDefinitions().get(key);
        ScopeType scope = findBeanScope(executable);

        if (scope == ScopeType.PROTOTYPE) {
            return beanFactory.createBeanInstance(executable);
        }

        // Handle singletons manually
        if (context.isSingletonRegistered(key)) {
            return (T) context.getSingleton(key);
        }

        context.markAsInCreation(key);
        try {
            T beanInstance = beanFactory.createBeanInstance(executable);
            context.registerSingleton(key, beanInstance);
            return beanInstance;
        } finally {
            context.unmarkAsInCreation(key);
        }
    }

    private static ScopeType findBeanScope(Executable executable) {
        for (Annotation annotation : executable.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(Bean.class.getName())) {
                try {
                    Method scopeMethod = annotation.annotationType().getMethod("scope");
                    return (ScopeType) scopeMethod.invoke(annotation);
                } catch (Exception e) { /* Ignore */ }
            }
        }
        if (executable instanceof java.lang.reflect.Constructor) {
            for (Annotation annotation : executable.getDeclaringClass().getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(Bean.class.getName())) {
                    try {
                        Method scopeMethod = annotation.annotationType().getMethod("scope");
                        return (ScopeType) scopeMethod.invoke(annotation);
                    } catch (Exception e) { /* Ignore */ }
                }
            }
        }
        return ScopeType.SINGLETON;
    }
}

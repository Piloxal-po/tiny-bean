package com.github.oxal.runner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.factory.BeanFactory;
import com.github.oxal.initializer.ContextInitializer;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.resolver.BeanDefinitionResolver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
public class ApplicationRunner {

    public static void loadContext(Class<?> application) {
        ContextInitializer.initialize(application);
    }

    public static <T> T loadBean(Class<T> beanClass) {
        return loadBean(beanClass, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadBean(Class<T> beanClass, String beanName) {
        log.trace("Entering loadBean(beanClass={}, beanName={})", beanClass.getName(), beanName);

        Context context = ContextService.getContext();
        if (beanClass.equals(Context.class)) {
            log.trace("Returning context instance directly.");
            return (T) context;
        }

        List<Object> manualCandidates = context.getSingletonInstances().entrySet().stream()
                .filter(entry -> beanClass.isAssignableFrom(entry.getKey().getType()))
                .filter(entry -> beanName == null || beanName.equals(entry.getKey().getName()))
                .map(Map.Entry::getValue)
                .toList();

        if (manualCandidates.size() == 1) {
            log.debug("Found unique manually registered singleton for type {}. Returning it directly.", beanClass.getName());
            return (T) manualCandidates.getFirst();
        }
        KeyDefinition key = BeanDefinitionResolver.resolve(beanClass, beanName, context);
        MDC.put("bean", key.toString());

        try {
            if (context.isBeanInCreation(key)) {
                log.error("Circular dependency detected for bean {}", key);
                throw new RuntimeException("Circular dependency detected for bean: " + key);
            }

            Executable executable = context.getBeanDefinitions().get(key);
            ScopeType scope = findBeanScope(executable);
            log.trace("Resolved bean [{}] with scope {}", key, scope);

            if (scope == ScopeType.PROTOTYPE) {
                log.debug("Creating new PROTOTYPE instance for bean [{}]", key);
                return BeanFactory.createBeanInstance(executable);
            }

            // Handle singletons
            if (context.isSingletonRegistered(key)) {
                log.debug("Returning cached SINGLETON instance for bean [{}]", key);
                return (T) context.getSingleton(key);
            }

            log.debug("Creating new SINGLETON instance for bean [{}]", key);
            context.markAsInCreation(key);
            try {
                T beanInstance = BeanFactory.createBeanInstance(executable);
                context.registerSingleton(key, beanInstance);
                log.debug("Successfully created and cached singleton bean [{}]", key);
                return beanInstance;
            } finally {
                context.unmarkAsInCreation(key);
            }
        } finally {
            MDC.remove("bean");
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

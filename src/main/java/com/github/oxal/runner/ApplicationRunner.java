package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;
import com.github.oxal.annotation.ScopeType;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.scanner.ApplicationScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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

        // --- 1. Perform a single scan of the classpath ---
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packages).scan()) {

            // --- 2. Execute @BeforeContextLoad callbacks ---
            ApplicationScanner.executeBeforeCallbacks(scanResult);

            // --- 3. Create the application context ---
            Context context = ContextService.createContexte(application, packages);

            // --- 4. Manually register ScanResult as a Singleton Bean ---
            context.getSingletonInstances().put(KeyDefinition.builder().type(ScanResult.class).build(), scanResult);

            // --- 5. Populate the context with beans and @After callbacks ---
            ApplicationScanner.populateContextFromScan(scanResult);
            System.out.println("Bean definitions found: " + context.getBeanDefinitions().size());

            // --- 6. Execute @AfterContextLoad callbacks ---
            executeAfterCallbacks(context);
        }
    }

    private static void executeAfterCallbacks(Context context) {
        for (Method callback : context.getAfterContextLoadCallbacks()) {
            try {
                callback.setAccessible(true);

                // The container class does not need to be a bean. We create a new instance for it.
                Object instance = callback.getDeclaringClass().getDeclaredConstructor().newInstance();

                // Resolve parameters for the callback method using the DI container
                Object[] args = new Object[callback.getParameterCount()];
                for (int i = 0; i < callback.getParameterCount(); i++) {
                    Class<?> paramType = callback.getParameterTypes()[i];
                    // Special case: inject the context itself
                    if (paramType.equals(Context.class)) {
                        args[i] = context;
                    } else {
                        Qualifier qualifier = callback.getParameters()[i].getAnnotation(Qualifier.class);
                        String qualifierName = (qualifier != null) ? qualifier.value() : null;
                        args[i] = loadBean(paramType, qualifierName);
                    }
                }
                callback.invoke(instance, args);

            } catch (Exception e) {
                throw new RuntimeException("Error executing @AfterContextLoad callback: " + callback.getName(), e);
            }
        }
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

        // Special case for manually registered singletons like ScanResult and Context
        if (beanClass.equals(Context.class)) {
            return (T) context;
        }
        if (context.getBeanDefinitionKey(beanClass, beanName).isEmpty()) {
            return (T) context.getSingletonInstances().entrySet().stream()
                    .filter(entry -> entry.getKey().sameType(beanClass) && (beanName == null || entry.getKey().sameName(beanName)))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No bean or manual singleton found for class: " + beanClass.getName()));
        }

        KeyDefinition key = context.getBeanDefinitionKey(beanClass, beanName).get();
        Executable executable = context.getBeanDefinitions().get(key);

        // Find the effective scope from the @Bean or stereotype annotation
        ScopeType scope = findBeanScope(executable);

        if (scope == ScopeType.PROTOTYPE) {
            return createBeanInstance(executable);
        }

        // Default to SINGLETON
        return (T) context.getSingletonInstances().computeIfAbsent(key, k -> createBeanInstance(executable));
    }

    private static ScopeType findBeanScope(Executable executable) {
        // Check on the executable itself (for method-based beans)
        for (Annotation annotation : executable.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(Bean.class.getName())) {
                try {
                    Method scopeMethod = annotation.annotationType().getMethod("scope");
                    return (ScopeType) scopeMethod.invoke(annotation);
                } catch (Exception e) { /* Ignore and continue */ }
            }
        }

        // Check on the class (for constructor-based beans)
        if (executable instanceof Constructor) {
            for (Annotation annotation : executable.getDeclaringClass().getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(Bean.class.getName())) {
                    try {
                        Method scopeMethod = annotation.annotationType().getMethod("scope");
                        return (ScopeType) scopeMethod.invoke(annotation);
                    } catch (Exception e) { /* Ignore and continue */ }
                }
            }
        }

        return ScopeType.SINGLETON; // Default scope
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

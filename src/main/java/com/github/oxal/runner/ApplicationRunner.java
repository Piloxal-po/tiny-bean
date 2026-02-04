package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;
import com.github.oxal.annotation.ScopeType;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.factory.BeanFactory;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.provider.PackageProvider;
import com.github.oxal.resolver.BeanDefinitionResolver;
import com.github.oxal.scanner.ApplicationScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

public class ApplicationRunner {

    private static final BeanFactory beanFactory = new BeanFactory();
    private static final BeanDefinitionResolver beanDefinitionResolver = new BeanDefinitionResolver();

    public static void loadContext(Class<?> application) {
        if (!application.isAnnotationPresent(Application.class)) {
            throw new RuntimeException(Application.class.getName() + " is not present in annotation of " + application.getName());
        }

        Application configuration = application.getAnnotation(Application.class);

        Set<String> packagesToScan = new HashSet<>();
        packagesToScan.add(application.getPackageName());
        packagesToScan.addAll(Arrays.asList(configuration.packages()));
        ServiceLoader<PackageProvider> loader = ServiceLoader.load(PackageProvider.class);
        for (PackageProvider provider : loader) {
            packagesToScan.addAll(Arrays.asList(provider.getPackages()));
        }

        String[] packages = packagesToScan.toArray(new String[0]);

        // --- Perform a single scan of the classpath ---
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packages).scan()) {

            // --- Execute @BeforeContextLoad callbacks ---
            ApplicationScanner.executeBeforeCallbacks(scanResult);

            // --- Create the application context ---
            Context context = ContextService.createContexte(application, packages);

            // --- Manually register ScanResult as a Singleton Bean ---
            context.getSingletonInstances().put(KeyDefinition.builder().type(ScanResult.class).build(), scanResult);

            // --- Populate the context with beans and @After callbacks ---
            ApplicationScanner.populateContextFromScan(scanResult);
            System.out.println("Bean definitions found: " + context.getBeanDefinitions().size());

            // --- Execute @AfterContextLoad callbacks ---
            executeAfterCallbacks(context);
        }
    }

    private static void executeAfterCallbacks(Context context) {
        for (Method callback : context.getAfterContextLoadCallbacks()) {
            try {
                callback.setAccessible(true);
                Object instance = callback.getDeclaringClass().getDeclaredConstructor().newInstance();
                Object[] args = new Object[callback.getParameterCount()];
                for (int i = 0; i < callback.getParameterCount(); i++) {
                    Class<?> paramType = callback.getParameterTypes()[i];
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
        if (beanClass.equals(Context.class)) {
            return (T) context;
        }

        KeyDefinition key = beanDefinitionResolver.resolve(beanClass, beanName, context);

        if (context.getBeansInCreation().contains(key)) {
            throw new RuntimeException("Circular dependency detected for bean: " + key);
        }

        Executable executable = context.getBeanDefinitions().get(key);
        ScopeType scope = findBeanScope(executable);

        if (scope == ScopeType.PROTOTYPE) {
            return beanFactory.createBeanInstance(executable);
        }

        // Handle singletons manually
        if (context.getSingletonInstances().containsKey(key)) {
            return (T) context.getSingletonInstances().get(key);
        }

        context.getBeansInCreation().add(key);
        try {
            T beanInstance = beanFactory.createBeanInstance(executable);
            context.getSingletonInstances().put(key, beanInstance);
            return beanInstance;
        } finally {
            context.getBeansInCreation().remove(key);
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

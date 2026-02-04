package com.github.oxal.initializer;

import com.github.oxal.annotation.Application;
import com.github.oxal.annotation.Qualifier;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.provider.PackageProvider;
import com.github.oxal.runner.ApplicationRunner;
import com.github.oxal.scanner.ApplicationScanner;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

public class ContextInitializer {

    public void initialize(Class<?> application) {
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
            context.registerSingleton(KeyDefinition.builder().type(ScanResult.class).build(), scanResult);

            // --- Populate the context with beans and @After callbacks ---
            ApplicationScanner.populateContextFromScan(scanResult);
            System.out.println("Bean definitions found: " + context.getBeanDefinitions().size());

            // --- Execute @AfterContextLoad callbacks ---
            executeAfterCallbacks(context);
        }
    }

    private void executeAfterCallbacks(Context context) {
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
                        args[i] = ApplicationRunner.loadBean(paramType, qualifierName);
                    }
                }
                callback.invoke(instance, args);
            } catch (Exception e) {
                throw new RuntimeException("Error executing @AfterContextLoad callback: " + callback.getName(), e);
            }
        }
    }
}

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
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

@Slf4j
public class ContextInitializer {

    public void initialize(Class<?> application) {
        log.info("Starting Tiny-Bean context for application: {}", application.getName());

        if (!application.isAnnotationPresent(Application.class)) {
            log.error("The application class {} is not annotated with @Application.", application.getName());
            throw new RuntimeException(Application.class.getName() + " is not present in annotation of " + application.getName());
        }

        Application configuration = application.getAnnotation(Application.class);

        Set<String> packagesToScan = new HashSet<>();
        packagesToScan.add(application.getPackageName());
        packagesToScan.addAll(Arrays.asList(configuration.packages()));
        log.debug("Base packages from @Application: {}", packagesToScan);

        ServiceLoader<PackageProvider> loader = ServiceLoader.load(PackageProvider.class);
        for (PackageProvider provider : loader) {
            log.debug("Discovered PackageProvider: {}", provider.getClass().getName());
            packagesToScan.addAll(Arrays.asList(provider.getPackages()));
        }

        String[] packages = packagesToScan.toArray(new String[0]);
        log.debug("Final packages to scan: {}", Arrays.toString(packages));

        long startTime = System.currentTimeMillis();
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packages).scan()) {
            log.info("Classpath scan completed in {}ms", System.currentTimeMillis() - startTime);

            log.debug("Executing @BeforeContextLoad callbacks...");
            ApplicationScanner.executeBeforeCallbacks(scanResult);

            Context context = ContextService.createContexte(application, packages);
            context.registerSingleton(KeyDefinition.builder().type(ScanResult.class).build(), scanResult);

            log.debug("Populating context with bean definitions and @After callbacks...");
            ApplicationScanner.populateContextFromScan(scanResult);
            log.info("Found {} bean definitions.", context.getBeanDefinitionCount());

            log.debug("Executing @AfterContextLoad callbacks...");
            executeAfterCallbacks(context);
        }
        log.info("Tiny-Bean context initialized successfully.");
    }

    private void executeAfterCallbacks(Context context) {
        for (Method callback : context.getAfterContextLoadCallbacks()) {
            log.debug("Executing @AfterContextLoad callback: {}", callback);
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
                log.error("Error executing @AfterContextLoad callback: {}", callback.getName(), e);
                throw new RuntimeException("Error executing @AfterContextLoad callback: " + callback.getName(), e);
            }
        }
    }
}

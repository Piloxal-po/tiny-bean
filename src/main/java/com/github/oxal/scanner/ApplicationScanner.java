package com.github.oxal.scanner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.context.AfterContextLoad;
import com.github.oxal.annotation.context.BeforeContextLoad;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationScanner {

    /**
     * Finds, sorts, and executes all @BeforeContextLoad callbacks.
     * This is done before the context is created.
     */
    public static void executeBeforeCallbacks(ScanResult scanResult) {
        List<Method> beforeCallbacks = scanResult.getClassesWithMethodAnnotation(BeforeContextLoad.class.getName())
                .stream()
                .flatMap(ci -> ci.getMethodInfo().filter(mi -> mi.hasAnnotation(BeforeContextLoad.class.getName())).stream())
                .map(MethodInfo::loadClassAndGetMethod)
                .peek(ApplicationScanner::validateBeforeCallback)
                .sorted(Comparator.comparingInt(m -> m.getAnnotation(BeforeContextLoad.class).order()))
                .collect(Collectors.toList());

        // Execute them immediately
        for (Method callback : beforeCallbacks) {
            try {
                callback.setAccessible(true);
                Object instance = callback.getDeclaringClass().getDeclaredConstructor().newInstance();
                if (callback.getParameterCount() == 1) {
                    callback.invoke(instance, scanResult);
                } else {
                    callback.invoke(instance);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error executing @BeforeContextLoad callback: " + callback.getName(), e);
            }
        }
    }

    /**
     * Populates the context with bean definitions and @AfterContextLoad callbacks.
     */
    public static void populateContextFromScan(ScanResult scanResult) {
        Context context = ContextService.getContext();
        scanBeans(scanResult, context);
        scanAfterCallbacks(scanResult, context);
    }

    private static void scanBeans(ScanResult scanResult, Context context) {
        // Scan for class beans
        scanResult.getClassesWithAnnotation(Bean.class.getName()).loadClasses(true).forEach(clazz -> {
            if (clazz.getConstructors().length > 1) {
                throw new RuntimeException("More than one constructor found for " + clazz.getSimpleName());
            }
            KeyDefinition key = KeyDefinition.builder().type(clazz).build();
            Bean bean = clazz.getAnnotation(Bean.class);
            if (bean != null && !Bean.DEFAULT.equals(bean.value())) {
                key.setName(bean.value());
            }
            context.addBeanDefinition(key, clazz.getConstructors()[0]);
        });

        // Scan for method beans
        scanResult.getClassesWithMethodAnnotation(Bean.class.getName()).forEach(classInfo ->
                classInfo.getMethodInfo().filter(mi -> mi.hasAnnotation(Bean.class.getName())).forEach(methodInfo -> {
                    Method method = methodInfo.loadClassAndGetMethod();
                    KeyDefinition key = KeyDefinition.builder().name(method.getName()).type(method.getReturnType()).build();
                    Bean bean = method.getAnnotation(Bean.class);
                    if (bean != null && !Bean.DEFAULT.equals(bean.value())) {
                        key.setName(bean.value());
                    }
                    context.addBeanDefinition(key, method);
                })
        );
    }

    private static void scanAfterCallbacks(ScanResult scanResult, Context context) {
        // AfterContextLoad callbacks
        List<Method> afterCallbacks = scanResult.getClassesWithMethodAnnotation(AfterContextLoad.class.getName())
                .stream()
                .flatMap(ci -> ci.getMethodInfo().filter(mi -> mi.hasAnnotation(AfterContextLoad.class.getName())).stream())
                .map(MethodInfo::loadClassAndGetMethod)
                .peek(ApplicationScanner::validateAfterCallback)
                .sorted(Comparator.comparingInt(m -> m.getAnnotation(AfterContextLoad.class).order()))
                .collect(Collectors.toList());
        context.setAfterContextLoadCallbacks(afterCallbacks);
    }

    private static void validateBeforeCallback(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new RuntimeException("@BeforeContextLoad method must be public: " + method.getName());
        }
        if (method.getParameterCount() > 1 || (method.getParameterCount() == 1 && !method.getParameterTypes()[0].equals(ScanResult.class))) {
            throw new RuntimeException("@BeforeContextLoad method must have 0 or 1 parameter of type ScanResult: " + method.getName());
        }
    }

    private static void validateAfterCallback(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new RuntimeException("@AfterContextLoad method must be public: " + method.getName());
        }
        // Validation is relaxed. The DI container will resolve parameters at runtime.
    }
}

package com.github.oxal.scanner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.context.AfterContextLoad;
import com.github.oxal.annotation.context.BeforeContextLoad;
import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;

public class ApplicationScanner {

    public static void executeBeforeCallbacks(ScanResult scanResult) {
        List<Method> beforeCallbacks = scanResult.getClassesWithMethodAnnotation(BeforeContextLoad.class.getName())
                .stream()
                .flatMap(ci -> ci.getMethodInfo().filter(mi -> mi.hasAnnotation(BeforeContextLoad.class.getName())).stream())
                .map(MethodInfo::loadClassAndGetMethod)
                .peek(ApplicationScanner::validateBeforeCallback)
                .sorted(Comparator.comparingInt(m -> m.getAnnotation(BeforeContextLoad.class).order()))
                .toList();

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

    public static void populateContextFromScan(ScanResult scanResult) {
        Context context = ContextService.getContext();
        scanBeans(scanResult, context);
        scanAfterCallbacks(scanResult, context);
    }

    private static void scanBeans(ScanResult scanResult, Context context) {
        String beanAnnotationName = Bean.class.getName();

        scanResult.getClassesWithAnnotation(beanAnnotationName).loadClasses(true).forEach(clazz -> {
            if (clazz.isAnnotation()) {
                return;
            }

            if (clazz.getConstructors().length > 1) {
                throw new RuntimeException("More than one constructor found for " + clazz.getSimpleName());
            }
            KeyDefinition key = KeyDefinition.builder().type(clazz).build();

            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(beanAnnotationName)) {
                    try {
                        Method valueMethod = annotation.annotationType().getMethod("value");
                        String value = (String) valueMethod.invoke(annotation);
                        if (!Bean.DEFAULT.equals(value)) {
                            key.setName(value);
                        }
                    } catch (NoSuchMethodException e) {
                        // Annotation doesn't have a 'value' attribute, ignore.
                    } catch (Exception e) {
                        throw new RuntimeException("Error reading bean properties", e);
                    }
                    break;
                }
            }
            context.addBeanDefinition(key, clazz.getConstructors()[0]);
        });

        scanResult.getClassesWithMethodAnnotation(beanAnnotationName).forEach(classInfo ->
                classInfo.getMethodInfo().filter(mi -> mi.hasAnnotation(beanAnnotationName)).forEach(methodInfo -> {
                    Method method = methodInfo.loadClassAndGetMethod();
                    KeyDefinition key = KeyDefinition.builder().name(method.getName()).type(method.getReturnType()).build();

                    for (Annotation annotation : method.getAnnotations()) {
                        if (annotation.annotationType().isAnnotationPresent(Bean.class) || annotation.annotationType().getName().equals(beanAnnotationName)) {
                            try {
                                Method valueMethod = annotation.annotationType().getMethod("value");
                                String value = (String) valueMethod.invoke(annotation);
                                if (!Bean.DEFAULT.equals(value)) {
                                    key.setName(value);
                                }
                            } catch (NoSuchMethodException e) {
                                // Annotation doesn't have a 'value' attribute, ignore.
                            } catch (Exception e) {
                                throw new RuntimeException("Error reading bean properties", e);
                            }
                            break;
                        }
                    }
                    context.addBeanDefinition(key, method);
                })
        );
    }

    private static void scanAfterCallbacks(ScanResult scanResult, Context context) {
        scanResult.getClassesWithMethodAnnotation(AfterContextLoad.class.getName())
                .stream()
                .flatMap(ci -> ci.getMethodInfo().filter(mi -> mi.hasAnnotation(AfterContextLoad.class.getName())).stream())
                .map(MethodInfo::loadClassAndGetMethod)
                .peek(ApplicationScanner::validateAfterCallback)
                .sorted(Comparator.comparingInt(m -> m.getAnnotation(AfterContextLoad.class).order()))
                .forEach(context::addAfterContextLoadCallback);
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
    }
}

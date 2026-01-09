package com.github.oxal.scanner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationScanner {

    public static void scanBeans(String... packages) {
        Map<KeyDefinition, Executable> classMap = getClassWithAnnotation(Bean.class, packages);
        Map<KeyDefinition, Executable> methodMap = getMethodWithAnnotation(Bean.class, packages);

        classMap.forEach((k, v) -> {
            Bean bean = k.getType().getAnnotation(Bean.class);
            if (bean != null && !Bean.DEFAULT.equals(bean.value())) {
                k.setName(bean.value());
            }
            ContextService.getContext().addBeanDefinition(k, v);
        });

        methodMap.forEach((k, v) -> {
            Bean bean = v.getAnnotation(Bean.class);
            if (bean != null && !Bean.DEFAULT.equals(bean.value())) {
                k.setName(bean.value());
            }
            ContextService.getContext().addBeanDefinition(k, v);
        });
    }

    private static Map<KeyDefinition, Executable> getClassWithAnnotation(Class<? extends Annotation> annotation, String... packages) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .acceptPackages(packages)
                .scan()) {
            return scanResult.getClassesWithAnnotation(annotation.getName())
                    .loadClasses(true)
                    .stream()
                    .map(c -> {
                        if (c.getConstructors().length > 1) {
                            throw new RuntimeException("More than one constructor found for " + c.getSimpleName());
                        }
                        return c;
                    })
                    .collect(Collectors.toMap(c -> KeyDefinition.builder().type(c).build(), c -> c.getConstructors()[0]));
        }
    }

    private static Map<KeyDefinition, Executable> getMethodWithAnnotation(Class<? extends Annotation> annotation, String... packages) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packages)
                .scan()) {
            return scanResult.getAllClasses().stream()
                    .flatMap(classInfo -> classInfo.getMethodInfo().stream())
                    .filter(methodInfo -> methodInfo.hasAnnotation(annotation.getName()))
                    .map(MethodInfo::loadClassAndGetMethod)
                    .collect(Collectors.toMap(
                            m -> KeyDefinition.builder().name(m.getName()).type(m.getReturnType()).build(),
                            m -> m,
                            (e1, e2) -> e1 // In case of duplicate keys, which should be handled by addBeanDefinition
                    ));
        }
    }
}

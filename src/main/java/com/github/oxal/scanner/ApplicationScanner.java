package com.github.oxal.scanner;

import com.github.oxal.annotation.Bean;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationScanner {

    protected static Map<KeyDefinition, Executable> getClassWithBean(String packageBase) {
        return getClassWithAnnotation(packageBase, Bean.class);
    }

    protected static Map<KeyDefinition, Executable> getMethodWithBean(String packageBase) {
        return getMethodWithAnnotation(packageBase, Bean.class);
    }

    protected static Map<KeyDefinition, Executable> getClassWithBean(String[] packages) {
        Map<KeyDefinition, Executable> clazz = new HashMap<>();
        for (String packageBase : packages) {
            clazz.putAll(getClassWithBean(packageBase));
        }
        return clazz;
    }

    protected static Map<KeyDefinition, Executable> getMethodWithBean(String[] packages) {
        Map<KeyDefinition, Executable> method = new HashMap<>();
        for (String packageBase : packages) {
            method.putAll(getMethodWithBean(packageBase));
        }
        return method;
    }

    public static void scanBeans(String[] packages) {
        Map<KeyDefinition, Executable> classMap = getClassWithBean(packages);
        Map<KeyDefinition, Executable> methodMap = getMethodWithBean(packages);

        classMap.forEach((k, v) -> {
            Bean bean = k.getType().getAnnotation(Bean.class);
            if (bean != null) {
                if (!Bean.DEFAULT.equals(bean.value())) {
                    k.setName(bean.value());
                }
                ContextService.getContext().addBean(k, v);
            }
        });

        methodMap.forEach((k, v) -> {
            Bean bean = v.getAnnotation(Bean.class);
            if (bean != null) {
                if (!Bean.DEFAULT.equals(bean.value())) {
                    k.setName(bean.value());
                }
                ContextService.getContext().addBean(k, v);
            }
        });
    }

    protected static Map<KeyDefinition, Executable> getClassWithAnnotation(String packageBase, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(packageBase, Scanners.TypesAnnotated, Scanners.SubTypes);
        return reflections.getTypesAnnotatedWith(annotation)
                .stream()
                .map(c -> {
                    if (c.getConstructors().length > 1) {
                        throw new RuntimeException("More than one constructor found for " + c.getSimpleName());
                    }
                    return c;
                })
                .collect(Collectors.toMap(c -> KeyDefinition.builder().type(c).build(), c -> c.getConstructors()[0]));
    }

    protected static Map<KeyDefinition, Executable> getMethodWithAnnotation(String packageBase, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(packageBase, Scanners.MethodsAnnotated);
        return reflections.getMethodsAnnotatedWith(annotation)
                .stream()
                .collect(Collectors.toMap(m -> KeyDefinition.builder().name(m.getName()).type(m.getReturnType()).build(), m -> m));
    }

    protected static void insertBean(Map<KeyDefinition, Executable> beans, KeyDefinition keyDefinition, Executable executable) {
        if (keyDefinition.getName() != null  && beans.keySet().stream().noneMatch(k -> k.sameName(keyDefinition.getName()))) {
            beans.put(keyDefinition, executable);
        } else if (beans.keySet().stream().noneMatch(k -> k.sameName(keyDefinition.getType().getSimpleName()))) {
            keyDefinition.setName(keyDefinition.getType().getSimpleName());
            beans.put(keyDefinition, executable);
        } else {
            throw new RuntimeException("Duplicate bean name: " + keyDefinition.getClass().getSimpleName());
        }
    }
}

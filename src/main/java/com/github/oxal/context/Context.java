package com.github.oxal.context;

import com.github.oxal.object.KeyDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Context {
    private final Class<?> application;
    private final String[] packages;
    private Map<KeyDefinition, Executable> beanDefinitions;
    private Map<KeyDefinition, Object> singletonInstances;
    private Set<KeyDefinition> beansInCreation;

    private List<Method> beforeContextLoadCallbacks;
    private List<Method> afterContextLoadCallbacks;

    public void addBeanDefinition(KeyDefinition keyDefinition, Executable executable) {
        if (keyDefinition.getName() != null && beanDefinitions.keySet().stream().anyMatch(k -> k.sameName(keyDefinition.getName()))) {
            throw new RuntimeException("Duplicate bean name: " + keyDefinition.getName());
        }
        if (keyDefinition.getName() == null) {
            keyDefinition.setName(keyDefinition.getType().getSimpleName());
        }
        beanDefinitions.put(keyDefinition, executable);
    }
}

package com.github.oxal.context;

import com.github.oxal.object.KeyDefinition;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class Context {
    private final Class<?> application;
    private final String[] packages;
    private final Map<KeyDefinition, Executable> beanDefinitions;
    private final Map<KeyDefinition, Object> singletonInstances;
    private final Set<KeyDefinition> beansInCreation;
    private final List<Method> beforeContextLoadCallbacks;
    private final List<Method> afterContextLoadCallbacks;

    public void addBeanDefinition(KeyDefinition keyDefinition, Executable executable) {
        if (keyDefinition.getName() != null && beanDefinitions.keySet().stream().anyMatch(k -> k.sameName(keyDefinition.getName()))) {
            throw new RuntimeException("Duplicate bean name: " + keyDefinition.getName());
        }
        if (keyDefinition.getName() == null) {
            keyDefinition.setName(keyDefinition.getType().getSimpleName());
        }
        beanDefinitions.put(keyDefinition, executable);
    }

    public void addAfterContextLoadCallback(Method callback) {
        afterContextLoadCallbacks.add(callback);
    }

    public void registerSingleton(KeyDefinition key, Object instance) {
        singletonInstances.put(key, instance);
    }

    public boolean isSingletonRegistered(KeyDefinition key) {
        return singletonInstances.containsKey(key);
    }

    public Object getSingleton(KeyDefinition key) {
        return singletonInstances.get(key);
    }

    public int getSingletonInstanceCount() {
        return singletonInstances.size();
    }

    public int getBeanDefinitionCount() {
        return beanDefinitions.size();
    }

    public void markAsInCreation(KeyDefinition key) {
        beansInCreation.add(key);
    }

    public void unmarkAsInCreation(KeyDefinition key) {
        beansInCreation.remove(key);
    }

    public boolean isBeanInCreation(KeyDefinition key) {
        return beansInCreation.contains(key);
    }
}

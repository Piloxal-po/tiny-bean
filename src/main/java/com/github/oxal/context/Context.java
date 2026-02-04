package com.github.oxal.context;

import com.github.oxal.object.KeyDefinition;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
@Slf4j
public class Context {
    private final Class<?> application;
    private final String[] packages;
    private final Map<KeyDefinition, Executable> beanDefinitions;
    private final Map<KeyDefinition, Object> singletonInstances;
    private final Set<KeyDefinition> beansInCreation;
    private final List<Method> beforeContextLoadCallbacks;
    private final List<Method> afterContextLoadCallbacks;

    public void addBeanDefinition(KeyDefinition keyDefinition, Executable executable) {
        log.debug("Adding bean definition: {}", keyDefinition);
        if (keyDefinition.getName() != null && beanDefinitions.keySet().stream().anyMatch(k -> k.sameName(keyDefinition.getName()))) {
            throw new RuntimeException("Duplicate bean name: " + keyDefinition.getName());
        }
        if (keyDefinition.getName() == null) {
            keyDefinition.setName(keyDefinition.getType().getSimpleName());
        }
        beanDefinitions.put(keyDefinition, executable);
    }

    public void addAfterContextLoadCallback(Method callback) {
        log.debug("Adding after-context-load callback: {}", callback);
        afterContextLoadCallbacks.add(callback);
    }

    public void registerSingleton(KeyDefinition key, Object instance) {
        log.debug("Registering singleton: {}", key);
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
        log.debug("Marking bean as in creation: {}", key);
        beansInCreation.add(key);
    }

    public void unmarkAsInCreation(KeyDefinition key) {
        log.debug("Unmarking bean as in creation: {}", key);
        beansInCreation.remove(key);
    }

    public boolean isBeanInCreation(KeyDefinition key) {
        return beansInCreation.contains(key);
    }
}

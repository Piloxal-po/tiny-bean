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

    public void addBeanDefinitionByMethod(Class<?> clazz, Class<?> type, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                log.debug("Manually adding bean definition via method: {}", method);
                addBeanDefinition(KeyDefinition.builder().type(type).build(), method);
                return;
            }
        }
        throw new RuntimeException("Method not found: " + methodName);
    }

    public void addBeanDefinitionByConstructor(Class<?> clazz, Class<?> type) {
        if (clazz.getConstructors().length != 1) {
            throw new RuntimeException("Class has more than one constructor: " + clazz.getName());
        }
        log.debug("Manually adding bean definition via constructor: {}", clazz.getConstructors()[0]);
        addBeanDefinition(KeyDefinition.builder().type(type).build(), clazz.getConstructors()[0]);
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

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
import java.util.Optional;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Context {
    private final Class<?> application;
    private final String[] packages;
    private Map<KeyDefinition, Executable> beanDefinitions;
    private Map<KeyDefinition, Object> singletonInstances;

    private List<Method> beforeContextLoadCallbacks;
    private List<Method> afterContextLoadCallbacks;

    public Optional<KeyDefinition> getBeanDefinitionKey(Class<?> type, String name) {
        List<KeyDefinition> keys = beanDefinitions
                .keySet()
                .stream()
                .filter(key -> key.sameType(type) && (name == null || key.sameName(name)))
                .toList();

        if (keys.size() > 1) {
            throw new RuntimeException("More than one bean definition found for type " + type.getName() + " and name " + name);
        }
        return keys.stream().findFirst();
    }

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

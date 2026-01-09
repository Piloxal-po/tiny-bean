package com.github.oxal.context;

import com.github.oxal.object.KeyDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Context {
    private final Class<?> application;
    private final String[] packages;
    private Map<KeyDefinition, Executable> beans;
    private Map<KeyDefinition, Object> beansComputed;

    public Executable getExecutable(Class<?> type) {
        return getExecutable(type, null);
    }

    public Executable getExecutable(Class<?> type, String name) {
        List<Executable> Definitions = beans
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().sameType(type) && (name == null || entry.getKey().sameName(name)))
                .map(Map.Entry::getValue)
                .toList();
        if (Definitions.isEmpty()) {
            throw new RuntimeException("No bean definition found for type " + type.getName());
        } else if (Definitions.size() > 1) {
            throw new RuntimeException("More than one bean definition found for type " + type.getName());
        }
        return Definitions.getFirst();
    }

    public <T> T getExecutableComputed(Class<T> type) {
        return getExecutableComputed(type, null);
    }

    public <T> T getExecutableComputed(Class<T> type, String name) {
        List<Object> Definitions = beansComputed
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().sameType(type) && (name == null || entry.getKey().sameName(name)))
                .map(Map.Entry::getValue)
                .toList();
        if (Definitions.isEmpty()) {
            throw new RuntimeException("No bean definition found for type " + type.getName());
        } else if (Definitions.size() > 1) {
            throw new RuntimeException("More than one bean definition found for type " + type.getName());
        }
        return (T) Definitions.getFirst();
    }

    public void addBeanComputed(Class<?> type, String name, Object Bean) {
        if(beansComputed.keySet().stream().noneMatch(key -> key.sameType(type) && (name == null || key.sameName(name)))) {
            beansComputed.put(KeyDefinition.builder().name(name).type(type).build(),  Bean);
        }

    }

    public void addBean(KeyDefinition keyDefinition, Executable executable) {
        if (keyDefinition.getName() != null  && beans.keySet().stream().noneMatch(k -> k.sameName(keyDefinition.getName()))) {
            beans.put(keyDefinition, executable);
        } else if (beans.keySet().stream().noneMatch(k -> k.sameName(keyDefinition.getType().getSimpleName()))) {
            keyDefinition.setName(keyDefinition.getType().getSimpleName());
            beans.put(keyDefinition, executable);
        } else {
            throw new RuntimeException("Duplicate bean name: " + keyDefinition.getClass().getSimpleName());
        }
    }

    public void deleteBean(Class<?> type, String name) {
        List<KeyDefinition> keys = beans.keySet().stream()
                .filter(key -> key.sameType(type) && (name == null || key.sameName(name)))
                .toList();

        if (keys.size() > 1) {
            throw new RuntimeException("More than one bean definition found for type " + type.getName());
        }
        keys.forEach(key -> beans.remove(key));
    }
}

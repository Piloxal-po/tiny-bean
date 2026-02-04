package com.github.oxal.resolver;

import com.github.oxal.annotation.Primary;
import com.github.oxal.context.Context;
import com.github.oxal.object.KeyDefinition;

import java.lang.reflect.Executable;
import java.util.List;
import java.util.stream.Collectors;

public class BeanDefinitionResolver {

    public KeyDefinition resolve(Class<?> beanClass, String beanName, Context context) {
        List<KeyDefinition> candidates = context.getBeanDefinitions().keySet().stream()
                .filter(key -> beanClass.isAssignableFrom(key.getType()))
                .collect(Collectors.toList());

        if (beanName != null) {
            candidates = candidates.stream().filter(key -> beanName.equals(key.getName())).toList();
            if (candidates.size() == 1) {
                return candidates.getFirst();
            }
        }

        if (candidates.isEmpty()) {
            throw new RuntimeException("No bean definition found for type " + beanClass.getName());
        }

        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        // Ambiguity exists, try to resolve with @Primary
        List<KeyDefinition> primaryCandidates = candidates.stream()
                .filter(key -> isPrimary(context.getBeanDefinitions().get(key)))
                .toList();

        if (primaryCandidates.size() == 1) {
            return primaryCandidates.getFirst();
        }

        if (primaryCandidates.size() > 1) {
            throw new RuntimeException("Multiple primary beans found for type " + beanClass.getName() + ": " + primaryCandidates);
        }

        throw new RuntimeException("Multiple beans found for type " + beanClass.getName() + " and none is marked as primary. Use @Qualifier to specify the bean name.");
    }

    private boolean isPrimary(Executable executable) {
        if (executable.isAnnotationPresent(Primary.class)) {
            return true;
        }
        return executable.getDeclaringClass().isAnnotationPresent(Primary.class);
    }
}

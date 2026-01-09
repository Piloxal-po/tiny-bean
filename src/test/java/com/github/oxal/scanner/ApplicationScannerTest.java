package com.github.oxal.scanner;

import com.github.oxal.context.Context;
import com.github.oxal.context.ContextService;
import com.github.oxal.object.KeyDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationScannerTest {

    private final String TEST_PACKAGE = "fr.test.context.base";

    @AfterEach
    void tearDown() {
        // Clean up the context to ensure test isolation
        Context context = ContextService.getContext();
        if (context != null) {
            context.getBeanDefinitions().clear();
            context.getSingletonInstances().clear();
        }
    }

    @Test
    void scanBeans_shouldPopulateContextWithBeanDefinitions() {
        // Given
        ContextService.createContexte(this.getClass(), new String[]{TEST_PACKAGE});

        // When
        ApplicationScanner.scanBeans(new String[]{TEST_PACKAGE});

        // Then
        Map<KeyDefinition, Executable> beanDefinitions = ContextService.getContext().getBeanDefinitions();
        assertNotNull(beanDefinitions, "Bean definitions map in context should not be null.");
        assertEquals(5, beanDefinitions.size(), "Context should contain 5 bean definitions in total.");

        // Check for a class bean definition
        assertTrue(beanDefinitions.keySet().stream()
                        .anyMatch(k -> k.getType().getSimpleName().equals("Bean2")),
                "A definition for Bean2 should exist.");

        // Check for a named method bean definition
        assertTrue(beanDefinitions.keySet().stream()
                        .anyMatch(k -> "test".equals(k.getName()) && k.getType().equals(String.class)),
                "A string bean definition named 'test' should exist.");
        
        // Ensure no instances have been created yet
        assertTrue(ContextService.getContext().getSingletonInstances().isEmpty(), "Scanning should not create any instances.");
    }
}

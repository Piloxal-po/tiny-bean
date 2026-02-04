package com.github.oxal.scanner;

import com.github.oxal.annotation.Application;
import com.github.oxal.context.ContextService;
import com.github.oxal.context.TestContextHelper;
import com.github.oxal.object.KeyDefinition;
import com.github.oxal.runner.ApplicationRunner;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Application(packages = "fr.test.context.base")
class ApplicationScannerTest {

    @AfterEach
    void tearDown() {
        TestContextHelper.cleanup();
    }

    @Test
    void scanBeans_shouldPopulateContextWithBeanDefinitions() {
        // Given
        ApplicationRunner.loadContext(this.getClass());

        // Then
        Map<KeyDefinition, Executable> beanDefinitions = ContextService.getContext().getBeanDefinitions();
        assertNotNull(beanDefinitions, "Bean definitions map in context should not be null.");
        assertEquals(6, beanDefinitions.size(), "Context should contain 5 bean definitions in total.");

        // Check for a class bean definition
        assertTrue(beanDefinitions.keySet().stream()
                        .anyMatch(k -> k.getType().getSimpleName().equals("Bean2")),
                "A definition for Bean2 should exist.");

        // Check for a named method bean definition
        assertTrue(beanDefinitions.keySet().stream()
                        .anyMatch(k -> "test".equals(k.getName()) && k.getType().equals(String.class)),
                "A string bean definition named 'test' should exist.");

        // Ensure no instances have been created yet
        assertEquals(1, ContextService.getContext().getSingletonInstances().size(), "Scanning create ScanResult singleton instance");
        assertEquals(ScanResult.class, ContextService.getContext().getSingletonInstances().get(KeyDefinition.builder().type(ScanResult.class).build()).getClass(), "Scanning create ScanResult singleton instance");
    }
}

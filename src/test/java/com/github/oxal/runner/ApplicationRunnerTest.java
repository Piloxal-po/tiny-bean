package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.context.ContextService;
import com.github.oxal.context.TestContextHelper;
import fr.test.context.base.Bean1;
import fr.test.context.base.Bean2;
import fr.test.context.callbacks.CallbackTestFixtures;
import fr.test.context.scope.PrototypeBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationRunnerTest {

    // --- Test Applications ---

    @BeforeEach
    void setup() {
        CallbackTestFixtures.reset();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.cleanup();
    }

    @Test
    void beforeContextLoad_callbacks_shouldExecuteInOrderAndReceiveScanResult() {
        // When
        ApplicationRunner.loadContext(CallbackApplication.class);

        // Then
        assertTrue(CallbackTestFixtures.beforeScanResultInjected, "ScanResult should be injected into the before-callback.");
        assertEquals(List.of("firstBefore", "secondBefore"), CallbackTestFixtures.beforeCallbackExecutionOrder, "Before-callbacks should execute in the correct order.");
    }

    @Test
    void afterContextLoad_callbacks_shouldExecuteInOrderAndReceiveDependencies() {
        // When
        ApplicationRunner.loadContext(CallbackApplication.class);

        // Then
        assertTrue(CallbackTestFixtures.afterContextInjected, "Context should be injected into the after-callback.");
        assertTrue(CallbackTestFixtures.afterBeanInjected, "A bean dependency should be injected into the after-callback.");
        assertEquals(List.of("firstAfter", "secondAfter"), CallbackTestFixtures.afterCallbackExecutionOrder, "After-callbacks should execute in the correct order.");
    }

    @Test
    void loadContext_shouldCountBeanDefinitions() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertEquals(5, ContextService.getContext().getBeanDefinitions().size());
    }

    @Test
    void loadBean_shouldCreateSingletonInstances() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(1, ContextService.getContext().getSingletonInstances().size(), "Only ScanResult should be present initially.");

        // Load a bean and its dependencies
        ApplicationRunner.loadBean(Bean1.class); // Depends on String "test3"
        assertEquals(3, ContextService.getContext().getSingletonInstances().size(), "Should have ScanResult, Bean1 and its String dependency");

        ApplicationRunner.loadBean(Bean2.class); // Depends on String "test2"
        assertEquals(5, ContextService.getContext().getSingletonInstances().size(), "Should have ScanResult, Bean1, Bean2 and their dependencies");

        ApplicationRunner.loadBean(String.class, "test");
        assertEquals(6, ContextService.getContext().getSingletonInstances().size(), "Should have all beans instantiated");
    }

    @Test
    void loadBean_shouldReturnSameInstance_forSingletonScope() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        Bean1 bean1_instance1 = ApplicationRunner.loadBean(Bean1.class);
        Bean1 bean1_instance2 = ApplicationRunner.loadBean(Bean1.class);
        assertSame(bean1_instance1, bean1_instance2, "Should return the same singleton instance");
    }

    @Test
    void loadBean_shouldReturnNewInstance_forPrototypeScope() {
        ApplicationRunner.loadContext(ScopeApplication.class);
        PrototypeBean instance1 = ApplicationRunner.loadBean(PrototypeBean.class);
        PrototypeBean instance2 = ApplicationRunner.loadBean(PrototypeBean.class);
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotSame(instance1, instance2, "Should return a new prototype instance each time");
        assertEquals(1, ContextService.getContext().getSingletonInstances().size(), "Prototype beans should not be cached (only ScanResult)");
    }

    @Application(packages = "fr.test.context.base")
    private static class ApplicationMain {
    }

    // --- Test Lifecycle ---

    @Application(packages = "fr.test.context.circular")
    private static class CircularApplication {
    }

    @Application(packages = "fr.test.context.missing")
    private static class MissingDependencyApplication {
    }

    // --- Callback Tests ---

    @Application(packages = "fr.test.context.defaultconstructor")
    private static class NoDefaultConstructorApplication {
    }

    @Application(packages = "fr.test.context.scope")
    private static class ScopeApplication {
    }


    // --- Core Functionality Tests ---

    @Application(packages = {"fr.test.context.scope", "fr.test.context.base"})
    private static class MultiPackageApplication {
    }

    @Application(packages = {"com.github.oxal.runner"})
    private static class SamePackageApplication {
    }

    @Application
    private static class NonePackageApplication {
    }

    @Application(packages = {"fr.test.context.callbacks", "fr.test.context.base"})
    private static class CallbackApplication {
    }
}

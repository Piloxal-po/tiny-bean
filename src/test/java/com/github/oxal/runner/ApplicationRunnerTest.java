package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.context.ContextService;
import com.github.oxal.context.TestContextHelper;
import fr.test.context.base.Bean1;
import fr.test.context.base.Bean2;
import fr.test.context.callbacks.CallbackTestFixtures;
import fr.test.context.circular.BeanA;
import fr.test.context.missing.BeanWithMissingDependency;
import fr.test.context.scope.PrototypeBean;
import fr.test.context.stereotype.StereotypeTestFixtures;
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
    void stereotype_shouldDiscoverBeans() {
        ApplicationRunner.loadContext(StereotypeApplication.class);
        assertEquals(3, ContextService.getContext().getBeanDefinitions().size(), "Should discover all 3 stereotype-related beans.");
    }

    @Test
    void stereotype_shouldRespectCustomName() {
        ApplicationRunner.loadContext(StereotypeApplication.class);
        // Load by class and name
        Object bean = ApplicationRunner.loadBean(StereotypeTestFixtures.StereotypeNamedBean.class, "theStereotypeBean");
        assertNotNull(bean);
    }

    @Test
    void stereotype_shouldRespectPrototypeScope() {
        ApplicationRunner.loadContext(StereotypeApplication.class);
        Object p1 = ApplicationRunner.loadBean(StereotypeTestFixtures.StereotypePrototypeBean.class);
        Object p2 = ApplicationRunner.loadBean(StereotypeTestFixtures.StereotypePrototypeBean.class);
        assertNotSame(p1, p2, "Stereotype with prototype scope should produce new instances.");
    }

    @Test
    void stereotype_beansShouldBeInjectable() {
        ApplicationRunner.loadContext(StereotypeApplication.class);
        StereotypeTestFixtures.DependentOnStereotypeBean dependent = ApplicationRunner.loadBean(StereotypeTestFixtures.DependentOnStereotypeBean.class);
        assertNotNull(dependent, "The dependent bean should be loaded.");
        assertNotNull(dependent.getDependency(), "The stereotype bean should be injected successfully.");
        assertInstanceOf(StereotypeTestFixtures.StereotypeNamedBean.class, dependent.getDependency());
    }

    @Test
    void beforeContextLoad_callbacks_shouldExecuteInOrderAndReceiveScanResult() {
        ApplicationRunner.loadContext(CallbackApplication.class);
        assertTrue(CallbackTestFixtures.beforeScanResultInjected, "ScanResult should be injected into the before-callback.");
        assertEquals(List.of("firstBefore", "secondBefore"), CallbackTestFixtures.beforeCallbackExecutionOrder, "Before-callbacks should execute in the correct order.");
    }


    // --- Test Lifecycle ---

    @Test
    void afterContextLoad_callbacks_shouldExecuteInOrderAndReceiveDependencies() {
        ApplicationRunner.loadContext(CallbackApplication.class);
        assertTrue(CallbackTestFixtures.afterContextInjected, "Context should be injected into the after-callback.");
        assertTrue(CallbackTestFixtures.afterBeanInjected, "A bean dependency should be injected into the after-callback.");
        assertEquals(List.of("firstAfter", "secondAfter"), CallbackTestFixtures.afterCallbackExecutionOrder, "After-callbacks should execute in the correct order.");
    }

    @Test
    void loadContext_shouldCountBeanDefinitions() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(5, ContextService.getContext().getBeanDefinitions().size());
    }

    // --- Stereotype Tests ---

    @Test
    void loadBean_shouldCreateSingletonInstances() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(1, ContextService.getContext().getSingletonInstances().size(), "Only ScanResult should be present initially.");

        ApplicationRunner.loadBean(Bean1.class);
        assertEquals(3, ContextService.getContext().getSingletonInstances().size());

        ApplicationRunner.loadBean(Bean2.class);
        assertEquals(5, ContextService.getContext().getSingletonInstances().size());

        ApplicationRunner.loadBean(String.class, "test");
        assertEquals(6, ContextService.getContext().getSingletonInstances().size());
    }

    @Test
    void loadBean_shouldReturnSameInstance_forSingletonScope() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        Bean1 bean1_instance1 = ApplicationRunner.loadBean(Bean1.class);
        Bean1 bean1_instance2 = ApplicationRunner.loadBean(Bean1.class);
        assertSame(bean1_instance1, bean1_instance2);
    }

    @Test
    void loadBean_shouldReturnNewInstance_forPrototypeScope() {
        ApplicationRunner.loadContext(ScopeApplication.class);
        PrototypeBean instance1 = ApplicationRunner.loadBean(PrototypeBean.class);
        PrototypeBean instance2 = ApplicationRunner.loadBean(PrototypeBean.class);
        assertNotSame(instance1, instance2);
        assertEquals(1, ContextService.getContext().getSingletonInstances().size());
    }

    @Test
    void loadBean_with_circular_dependency_should_fail() {
        ApplicationRunner.loadContext(CircularApplication.class);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(BeanA.class));
    }


    // --- Callback Tests ---

    @Test
    void loadBean_with_missing_dependency_should_fail() {
        ApplicationRunner.loadContext(MissingDependencyApplication.class);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(BeanWithMissingDependency.class));
    }

    @Test
    void loadBean_from_method_in_class_without_default_constructor_should_fail() {
        ApplicationRunner.loadContext(NoDefaultConstructorApplication.class);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(String.class));
    }


    // --- Core Functionality Tests ---

    @Application(packages = "fr.test.context.base")
    private static class ApplicationMain {
    }

    @Application(packages = "fr.test.context.circular")
    private static class CircularApplication {
    }

    @Application(packages = "fr.test.context.missing")
    private static class MissingDependencyApplication {
    }

    @Application(packages = "fr.test.context.defaultconstructor")
    private static class NoDefaultConstructorApplication {
    }

    // --- Error Handling Tests ---

    @Application(packages = "fr.test.context.scope")
    private static class ScopeApplication {
    }

    @Application(packages = {"fr.test.context.callbacks", "fr.test.context.base"})
    private static class CallbackApplication {
    }

    @Application(packages = "fr.test.context.stereotype")
    private static class StereotypeApplication {
    }
}

package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.context.ContextService;
import com.github.oxal.context.TestContextHelper;
import fr.test.context.base.Bean1;
import fr.test.context.base.Bean2;
import fr.test.context.callbacks.CallbackTestFixtures;
import fr.test.context.circular.BeanA;
import fr.test.context.configuration.ConfigurationTestFixtures;
import fr.test.context.external.ExternalBean;
import fr.test.context.list.ListInjectionTestFixtures;
import fr.test.context.missing.BeanWithMissingDependency;
import fr.test.context.primary.common.PrimaryTestFixtures;
import fr.test.context.primary.success.SuccessFixtures;
import fr.test.context.scope.PrototypeBean;
import fr.test.context.set.SetInjectionTestFixtures;
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
        SetInjectionTestFixtures.CallbackBean.injectedServices = null;
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.cleanup();
    }

    @Test
    void stereotype_shouldDiscoverBeans() {
        ApplicationRunner.loadContext(StereotypeApplication.class);
        assertEquals(4, ContextService.getContext().getBeanDefinitionCount(), "Should discover all 3 stereotype-related beans.");
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
        assertEquals(7, ContextService.getContext().getBeanDefinitionCount());
    }

    // --- Stereotype Tests ---

    @Test
    void loadBean_shouldCreateSingletonInstances() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(1, ContextService.getContext().getSingletonInstanceCount(), "Only ScanResult should be present initially.");

        ApplicationRunner.loadBean(Bean1.class);
        assertEquals(3, ContextService.getContext().getSingletonInstanceCount());

        ApplicationRunner.loadBean(Bean2.class);
        assertEquals(5, ContextService.getContext().getSingletonInstanceCount());

        ApplicationRunner.loadBean(String.class, "test");
        assertEquals(6, ContextService.getContext().getSingletonInstanceCount());
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
        assertEquals(1, ContextService.getContext().getSingletonInstanceCount());
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

    @Test
    void serviceLoader_shouldLoadExternalBeans() {
        ApplicationRunner.loadContext(ServiceLoaderApplication.class);
        ExternalBean externalBean = ApplicationRunner.loadBean(ExternalBean.class);
        assertNotNull(externalBean, "Bean from ServiceLoader-provided package should be loaded.");
    }

    // --- @Primary Tests ---

    @Test
    void primary_shouldInjectPrimaryBean_whenMultipleCandidatesExist() {
        ApplicationRunner.loadContext(PrimaryApplication.class);
        PrimaryTestFixtures.ServiceConsumer consumer = ApplicationRunner.loadBean(PrimaryTestFixtures.ServiceConsumer.class);
        assertNotNull(consumer);
        assertInstanceOf(SuccessFixtures.PrimaryPrimaryService.class, consumer.getService());
    }

    @Test
    void primary_shouldThrowException_whenMultiplePrimaryBeansExist() {
        ApplicationRunner.loadContext(MultiplePrimaryApplication.class);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(PrimaryTestFixtures.PrimaryTestService.class));
    }

    @Test
    void primary_shouldThrowException_whenAmbiguityExistsAndNoPrimaryBean() {
        ApplicationRunner.loadContext(NoPrimaryApplication.class);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(PrimaryTestFixtures.PrimaryTestService.class));
    }

    // --- @Configuration Tests ---

    @Test
    void configuration_shouldInjectProperties() {
        ApplicationRunner.loadContext(ConfigurationApplication.class);

        ConfigurationTestFixtures.ServerConfig serverConfig = ApplicationRunner.loadBean(ConfigurationTestFixtures.ServerConfig.class);
        assertEquals(8080, serverConfig.getPort());
        assertEquals("localhost", serverConfig.getHost());

        ConfigurationTestFixtures.AppConfig appConfig = ApplicationRunner.loadBean(ConfigurationTestFixtures.AppConfig.class);
        assertEquals("TinyBeanApp", appConfig.getName());
        assertEquals("1.0.0", appConfig.getVersion());

        ConfigurationTestFixtures.FeatureConfig featureConfig = ApplicationRunner.loadBean(ConfigurationTestFixtures.FeatureConfig.class);
        assertTrue(featureConfig.isEnabled());

        ConfigurationTestFixtures.DatabaseConfig dbConfig = ApplicationRunner.loadBean(ConfigurationTestFixtures.DatabaseConfig.class);
        assertEquals("jdbc:h2:mem:test", dbConfig.getUrl());
        assertEquals("sa", dbConfig.getUsername());
        assertNotNull(dbConfig.getConnection());
        assertEquals(10, dbConfig.getConnection().getMax());
    }

    // --- List Injection Tests ---

    @Test
    void listInjection_shouldInjectAllBeansOfType() {
        ApplicationRunner.loadContext(ListInjectionApplication.class);
        ListInjectionTestFixtures.PluginManager manager = ApplicationRunner.loadBean(ListInjectionTestFixtures.PluginManager.class);

        assertNotNull(manager);
        assertNotNull(manager.getPlugins());
        assertEquals(2, manager.getPlugins().size());

        List<String> names = manager.getPlugins().stream().map(ListInjectionTestFixtures.MyPlugin::getName).toList();
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));
    }
    
    @Test
    void setInjection_shouldInjectAllBeansOfType() {
        ApplicationRunner.loadContext(SetInjectionApplication.class);
        assertNotNull(SetInjectionTestFixtures.CallbackBean.injectedServices);
        assertEquals(2, SetInjectionTestFixtures.CallbackBean.injectedServices.size());
        
        List<String> names = SetInjectionTestFixtures.CallbackBean.injectedServices.stream().map(SetInjectionTestFixtures.MyService::getName).toList();
        assertTrue(names.contains("A"));
        assertTrue(names.contains("B"));
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

    @Application
    private static class ServiceLoaderApplication {
    }

    @Application(packages = {"fr.test.context.primary.common", "fr.test.context.primary.success"})
    private static class PrimaryApplication {
    }

    @Application(packages = {"fr.test.context.primary.common", "fr.test.context.primary.multiple"})
    private static class MultiplePrimaryApplication {
    }

    @Application(packages = {"fr.test.context.primary.common", "fr.test.context.primary.ambiguous"})
    private static class NoPrimaryApplication {
    }

    @Application(packages = "fr.test.context.configuration")
    private static class ConfigurationApplication {
    }

    @Application(packages = "fr.test.context.list")
    private static class ListInjectionApplication {
    }

    @Application(packages = "fr.test.context.set")
    private static class SetInjectionApplication {
    }
}

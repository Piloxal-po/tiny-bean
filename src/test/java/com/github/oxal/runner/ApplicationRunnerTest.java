package com.github.oxal.runner;

import com.github.oxal.annotation.Application;
import com.github.oxal.context.ContextService;
import com.github.oxal.context.TestContextHelper;
import fr.test.context.base.Bean1;
import fr.test.context.base.Bean2;
import fr.test.context.circular.BeanA;
import fr.test.context.missing.BeanWithMissingDependency;
import fr.test.context.scope.PrototypeBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationRunnerTest {

    @AfterEach
    void cleanContext() {
        TestContextHelper.cleanup();
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
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertEquals(0, ContextService.getContext().getSingletonInstances().size());

        // Load a bean and its dependencies
        ApplicationRunner.loadBean(Bean1.class); // Depends on String "test3"
        assertEquals(2, ContextService.getContext().getSingletonInstances().size(), "Should have Bean1 and its String dependency");

        ApplicationRunner.loadBean(Bean2.class); // Depends on String "test2"
        assertEquals(4, ContextService.getContext().getSingletonInstances().size(), "Should have Bean1, Bean2 and their dependencies");

        ApplicationRunner.loadBean(String.class, "test");
        assertEquals(5, ContextService.getContext().getSingletonInstances().size(), "Should have all beans instantiated");
    }

    @Test
    void loadBean_shouldReturnSameInstance_forSingletonScope() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        Bean1 bean1_instance1 = ApplicationRunner.loadBean(Bean1.class);
        Bean1 bean1_instance2 = ApplicationRunner.loadBean(Bean1.class);
        assertSame(bean1_instance1, bean1_instance2, "Should return the same singleton instance");
    }

    @Test
    void loadBean_shouldReturnNewInstance_forPrototypeScope() {
        ApplicationRunner.loadContext(ScopeApplication.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        PrototypeBean instance1 = ApplicationRunner.loadBean(PrototypeBean.class);
        PrototypeBean instance2 = ApplicationRunner.loadBean(PrototypeBean.class);
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotSame(instance1, instance2, "Should return a new prototype instance each time");
        assertTrue(ContextService.getContext().getSingletonInstances().isEmpty(), "Prototype beans should not be cached");
    }

    @Test
    void loadContext_bean_defines_in_other_file() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        Bean1 bean1 = ApplicationRunner.loadBean(Bean1.class);
        assertNotNull(bean1);
        assertEquals("je ne sais pas test3", bean1.getTest());
    }

    @Test
    void loadContext_bean_defines_in_same_file() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        Bean2 bean2 = ApplicationRunner.loadBean(Bean2.class);
        assertNotNull(bean2);
        assertEquals("je m'en fou test2", bean2.getTest());
    }

    @Test
    void loadContext_bean_with_multiple_bean_same_classe_with_name() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        String bean = ApplicationRunner.loadBean(String.class, "test");
        assertNotNull(bean);
        assertEquals("hello world test", bean);
    }

    @Test
    void loadContext_bean_with_multiple_bean_same_classe_without_name() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(String.class));
    }

    @Test
    void loadContext_bean_with_multiple_bean_same_classe_with_wrong_name() {
        ApplicationRunner.loadContext(ApplicationMain.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(String.class, "lqsjdlqkdjqlkj"));
    }

    @Test
    void loadBean_with_circular_dependency_should_fail() {
        ApplicationRunner.loadContext(CircularApplication.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(BeanA.class));
    }

    @Test
    void loadBean_with_missing_dependency_should_fail() {
        ApplicationRunner.loadContext(MissingDependencyApplication.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(BeanWithMissingDependency.class));
    }

    @Test
    void loadBean_from_method_in_class_without_default_constructor_should_fail() {
        ApplicationRunner.loadContext(NoDefaultConstructorApplication.class);
        assertEquals(2, ContextService.getContext().getPackages().length);
        assertThrows(RuntimeException.class, () -> ApplicationRunner.loadBean(String.class));
    }

    @Test
    void count_packages_multi_package() {
        ApplicationRunner.loadContext(MultiPackageApplication.class);
        assertEquals(3, ContextService.getContext().getPackages().length);
    }

    @Test
    void count_packages_none_package() {
        ApplicationRunner.loadContext(NonePackageApplication.class);
        assertEquals(1, ContextService.getContext().getPackages().length);
    }

    @Test
    void count_packages_same_package() {
        ApplicationRunner.loadContext(SamePackageApplication.class);
        assertEquals(1, ContextService.getContext().getPackages().length);
    }

    @Application(packages = "fr.test.context.base")
    private static class ApplicationMain {
    }

    @Application(packages = "fr.test.context.circular")
    private static class CircularApplication {
    }

    @Application(packages = {
            "fr.test.context.scope",
            "fr.test.context.base"
    })
    private static class MultiPackageApplication {
    }

    @Application(packages = {
            "com.github.oxal.runner"
    })
    private static class SamePackageApplication {
    }

    @Application
    private static class NonePackageApplication {
    }

    // --- Error Handling Tests ---

    @Application(packages = "fr.test.context.missing")
    private static class MissingDependencyApplication {
    }

    @Application(packages = "fr.test.context.defaultconstructor")
    private static class NoDefaultConstructorApplication {
    }

    @Application(packages = "fr.test.context.scope")
    private static class ScopeApplication {
    }
}

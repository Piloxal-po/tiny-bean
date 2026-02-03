package fr.test.context.stereotype;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class StereotypeTestFixtures {

    /**
     * A custom stereotype annotation that is meta-annotated with @Bean.
     * It re-declares the 'value' and 'scope' attributes to show they can be customized.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Bean // Meta-annotation
    public @interface TestService {
        String value() default Bean.DEFAULT;

        ScopeType scope() default ScopeType.SINGLETON;
    }

    /**
     * A bean using the stereotype with a custom name.
     */
    @TestService("theStereotypeBean")
    public static class StereotypeNamedBean {
    }

    /**
     * A bean using the stereotype with a prototype scope.
     */
    @TestService(scope = ScopeType.PROTOTYPE)
    public static class StereotypePrototypeBean {
    }

    /**
     * A regular bean that depends on the stereotype-annotated bean.
     * This is used to verify that stereotype beans can be injected.
     */
    @Bean
    public static class DependentOnStereotypeBean {
        private final StereotypeNamedBean dependency;

        public DependentOnStereotypeBean(StereotypeNamedBean dependency) {
            this.dependency = dependency;
        }

        public StereotypeNamedBean getDependency() {
            return dependency;
        }
    }
}

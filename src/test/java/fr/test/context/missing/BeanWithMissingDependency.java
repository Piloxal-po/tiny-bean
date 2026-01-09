package fr.test.context.missing;

import com.github.oxal.annotation.Bean;

@Bean
public class BeanWithMissingDependency {
    public BeanWithMissingDependency(MissingBean missingBean) {
    }
}

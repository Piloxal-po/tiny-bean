package fr.test.context.circular;

import com.github.oxal.annotation.Bean;

@Bean
public class BeanB {
    public BeanB(BeanA beanA) {
    }
}

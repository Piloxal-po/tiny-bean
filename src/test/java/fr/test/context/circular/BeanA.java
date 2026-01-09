package fr.test.context.circular;

import com.github.oxal.annotation.Bean;

@Bean
public class BeanA {
    public BeanA(BeanB beanB) {
    }
}

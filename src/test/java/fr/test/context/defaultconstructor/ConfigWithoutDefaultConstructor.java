package fr.test.context.defaultconstructor;

import com.github.oxal.annotation.Bean;

public class ConfigWithoutDefaultConstructor {

    public ConfigWithoutDefaultConstructor(String any) {
        // No default constructor
    }

    @Bean
    public String myBean() {
        return "hello";
    }
}

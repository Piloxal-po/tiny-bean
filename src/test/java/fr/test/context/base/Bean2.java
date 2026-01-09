package fr.test.context.base;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;
import lombok.Getter;

@Bean
@Getter
public class Bean2 {

    String test;

    public Bean2(@Qualifier("test2") String test) {
        this.test = test;
    }
}

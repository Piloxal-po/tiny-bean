package fr.test.context.base;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;

public class Method {

    @Bean
    public String test(){
        return "hello world test";
    }

    @Bean
    public String test2(){
        return "je m'en fou test2";
    }

    @Bean("test3")
    public String menfou(){
        return "je ne sais pas test3";
    }

    @Bean
    public Bean1 testBean(@Qualifier("test3") String test){
        return new Bean1(test);
    }
}

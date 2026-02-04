package fr.test.context.primary.multiple;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Primary;
import fr.test.context.primary.common.PrimaryTestFixtures;

public class MultiplePrimaryFixtures {

    @Bean
    @Primary
    public static class FirstPrimary implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "first";
        }
    }

    @Bean
    @Primary
    public static class SecondPrimary implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "second";
        }
    }
}

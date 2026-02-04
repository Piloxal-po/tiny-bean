package fr.test.context.primary.ambiguous;

import com.github.oxal.annotation.Bean;
import fr.test.context.primary.common.PrimaryTestFixtures;

public class AmbiguousFixtures {

    @Bean
    public static class FirstCandidate implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "first";
        }
    }

    @Bean
    public static class SecondCandidate implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "second";
        }
    }
}

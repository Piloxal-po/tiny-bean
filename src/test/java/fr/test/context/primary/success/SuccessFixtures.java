package fr.test.context.primary.success;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Primary;
import fr.test.context.primary.common.PrimaryTestFixtures;

public class SuccessFixtures {

    @Bean("defaultService")
    public static class DefaultPrimaryService implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "default";
        }
    }

    @Primary
    @Bean("primaryService")
    public static class PrimaryPrimaryService implements PrimaryTestFixtures.PrimaryTestService {
        @Override
        public String serve() {
            return "primary";
        }
    }
}

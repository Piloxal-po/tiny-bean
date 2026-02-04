package fr.test.context.primary.common;

import com.github.oxal.annotation.Bean;

public class PrimaryTestFixtures {

    // The interface to be injected
    public interface PrimaryTestService {
        String serve();
    }

    // A bean that depends on the interface
    @Bean
    public static class ServiceConsumer {
        private final PrimaryTestService service;

        public ServiceConsumer(PrimaryTestService service) {
            this.service = service;
        }

        public PrimaryTestService getService() {
            return service;
        }
    }
}

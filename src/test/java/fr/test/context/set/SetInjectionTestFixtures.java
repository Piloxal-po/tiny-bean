package fr.test.context.set;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.context.AfterContextLoad;

import java.util.Set;

public class SetInjectionTestFixtures {

    public interface MyService {
        String getName();
    }

    @Bean
    public static class ServiceA implements MyService {
        @Override
        public String getName() {
            return "A";
        }
    }

    @Bean
    public static class ServiceB implements MyService {
        @Override
        public String getName() {
            return "B";
        }
    }

    public static class CallbackBean {
        public static Set<MyService> injectedServices;

        @AfterContextLoad
        public void onStartup(Set<MyService> services) {
            injectedServices = services;
        }
    }
}

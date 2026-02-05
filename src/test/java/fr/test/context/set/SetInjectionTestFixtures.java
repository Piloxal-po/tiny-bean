package fr.test.context.set;

import com.github.oxal.annotation.Bean;

import java.util.Set;

public class SetInjectionTestFixtures {

    public interface MyPlugin {
        String getName();
    }

    @Bean
    public static class PluginA implements MyPlugin {
        @Override
        public String getName() {
            return "A";
        }
    }

    @Bean
    public static class PluginB implements MyPlugin {
        @Override
        public String getName() {
            return "B";
        }
    }

    @Bean
    public static class PluginManager {
        private final Set<MyPlugin> plugins;

        public PluginManager(Set<MyPlugin> plugins) {
            this.plugins = plugins;
        }

        public Set<MyPlugin> getPlugins() {
            return plugins;
        }
    }
}

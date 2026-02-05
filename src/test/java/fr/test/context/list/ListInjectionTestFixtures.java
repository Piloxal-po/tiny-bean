package fr.test.context.list;

import com.github.oxal.annotation.Bean;

import java.util.List;

public class ListInjectionTestFixtures {

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
        private final List<MyPlugin> plugins;

        public PluginManager(List<MyPlugin> plugins) {
            this.plugins = plugins;
        }

        public List<MyPlugin> getPlugins() {
            return plugins;
        }
    }
}

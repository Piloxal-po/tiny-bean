package fr.test.context.configuration;

import com.github.oxal.annotation.Configuration;

public class ConfigurationTestFixtures {

    @Configuration(prefix = "server")
    public static class ServerConfig {
        private int port;
        private String host;

        public int getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }
    }

    @Configuration(prefix = "app")
    public static class AppConfig {
        private String name;
        private String version;

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }

    @Configuration(prefix = "feature")
    public static class FeatureConfig {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }
    }

    @Configuration(prefix = "database")
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;

        private ConnectionConfig connection;

        public String getUrl() {
            return url;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public ConnectionConfig getConnection() {
            return connection;
        }

        public static class ConnectionConfig {
            private int max;

            public int getMax() {
                return max;
            }
        }
    }
}

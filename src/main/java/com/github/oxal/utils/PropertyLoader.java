package com.github.oxal.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertyLoader {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    public static void loadProperties() {
        if (loaded) {
            return;
        }
        try (InputStream input = PropertyLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                log.info("application.properties not found");
                return;
            }
            properties.load(input);
            loaded = true;
            log.info("Loaded {} properties from application.properties", properties.size());
        } catch (IOException ex) {
            log.error("Error loading application.properties", ex);
        }
    }

    public static String getProperty(String key) {
        if (!loaded) {
            loadProperties();
        }
        return properties.getProperty(key);
    }
}

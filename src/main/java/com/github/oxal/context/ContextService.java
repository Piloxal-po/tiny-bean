package com.github.oxal.context;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ContextService {

    private static final Object lock = new Object();
    private static volatile Context instance;

    public static Context createContexte(Class<?> application, String[] packages) {
        log.debug("Creating context for application {}", application.getName());
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = Context.builder()
                            .application(application)
                            .packages(packages)
                            .singletonInstances(new ConcurrentHashMap<>())
                            .beanDefinitions(new ConcurrentHashMap<>())
                            .beansInCreation(ConcurrentHashMap.newKeySet())
                            .beforeContextLoadCallbacks(new ArrayList<>())
                            .afterContextLoadCallbacks(new ArrayList<>())
                            .build();
                }
            }
        }
        return instance;
    }

    static void deleteContexte() {
        if (instance != null) {
            synchronized (lock) {
                if (instance != null) {
                    instance = null;
                }
            }
        }
    }

    public static Context getContext() {
        if (instance == null) {
            throw new IllegalStateException("Context has not been initialized. Call createContexte first.");
        }
        return instance;
    }
}

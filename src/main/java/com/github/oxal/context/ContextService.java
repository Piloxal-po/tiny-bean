package com.github.oxal.context;

import java.util.concurrent.ConcurrentHashMap;

public class ContextService {

    private static volatile Context instance;
    private static final Object lock = new Object();

    public static Context createContexte(Class<?> application, String[] packages) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = Context.builder()
                            .application(application)
                            .packages(packages)
                            .singletonInstances(new ConcurrentHashMap<>())
                            .beanDefinitions(new ConcurrentHashMap<>())
                            .build();
                }
            }
        }
        return instance;
    }

    public static Context getContext() {
        if (instance == null) {
            throw new IllegalStateException("Context has not been initialized. Call createContexte first.");
        }
        return instance;
    }
}

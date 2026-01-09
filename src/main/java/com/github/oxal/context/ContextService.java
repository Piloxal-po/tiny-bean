package com.github.oxal.context;

import java.util.HashMap;

public class ContextService {

    private static Context instance;

    public static Context createContexte(Class<?> application, String[] packages) {
        if (instance == null) {
            instance = Context.builder()
                    .application(application)
                    .packages(packages)
                    .beansComputed(new HashMap<>())
                    .beans(new HashMap<>())
                    .build();
        }
        return instance;
    }

    public static Context getContext() {
        return instance;
    }
}

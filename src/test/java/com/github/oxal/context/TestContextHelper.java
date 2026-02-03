package com.github.oxal.context;

/**
 * A test utility class to provide access to package-private testing methods in the context package.
 */
public class TestContextHelper {
    public static void cleanup() {
        ContextService.deleteContexte();
    }
}

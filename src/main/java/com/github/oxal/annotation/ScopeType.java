package com.github.oxal.annotation;

public enum ScopeType {
    /**
     * (Default) Scopes a single bean definition to a single object instance per Spring IoC container.
     */
    SINGLETON,

    /**
     * Scopes a single bean definition to any number of object instances.
     */
    PROTOTYPE
}

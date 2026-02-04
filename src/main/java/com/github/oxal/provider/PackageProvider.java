package com.github.oxal.provider;

/**
 * A service provider interface for libraries that want to automatically register
 * packages to be scanned by the Tiny-Bean context.
 *
 * <p>Implementations of this interface will be discovered automatically via
 * Java's {@link java.util.ServiceLoader} mechanism.
 */
public interface PackageProvider {

    /**
     * Returns an array of package names that should be added to the classpath scan.
     *
     * @return An array of package names.
     */
    String[] getPackages();
}

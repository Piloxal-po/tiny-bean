package fr.test.context.provider;

import com.github.oxal.provider.PackageProvider;

public class TestPackageProvider implements PackageProvider {
    @Override
    public String[] getPackages() {
        return new String[]{"fr.test.context.external"};
    }
}

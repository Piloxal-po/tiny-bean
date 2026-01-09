package fr.test.context.scope;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;

@Bean(scope = ScopeType.PROTOTYPE)
public class PrototypeBean {
    public PrototypeBean() {
        // A simple prototype bean
    }
}

package fr.test.context.manual;

public class ManualBeanTestFixtures {

    public static class ManualConstructorBean {
        public String sayHello() {
            return "Hello from constructor bean";
        }
    }

    public static class ManualConfig {
        public ManualMethodBean myMethodBean() {
            return new ManualMethodBean();
        }
    }

    public static class ManualMethodBean {
        public String sayHello() {
            return "Hello from method bean";
        }
    }
}

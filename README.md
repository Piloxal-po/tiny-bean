# Tiny-Bean: A Lightweight Dependency Injection Framework

[![](https://jitpack.io/v/Piloxal-po/tiny-bean.svg)](https://jitpack.io/#Piloxal-po/tiny-bean)

**Tiny-Bean** is a minimalist, annotation-driven Inversion of Control (IoC) container for Java. Inspired by the core
principles of frameworks like Spring, it provides a simple yet powerful way to manage your application's components and
their dependencies.

It is designed to be easy to understand and lightweight, with minimal, carefully chosen dependencies.

---

## Core Features

- **Annotation-Driven:** Configure your application using simple annotations like `@Application`, `@Bean`,
  `@Qualifier`, and `@Primary`.
- **Stereotype Annotations:** Create your own annotations (like `@Service` or `@Component`) that act as aliases for
  `@Bean`.
- **Lifecycle Callbacks:** Hook into the application startup process with `@BeforeContextLoad` and `@AfterContextLoad`.
- **Classpath Scanning:** Automatically discovers and registers your beans from specified packages.
- **Extensible Scanning:** Use the `ServiceLoader` pattern to add more packages to the scan from separate modules.
- **Dependency Injection:** Supports constructor-based dependency injection.
- **Bean Scopes:** Provides support for `SINGLETON` (default) and `PROTOTYPE` scopes.
- **Thread-Safe:** The context and bean loading mechanism are fully thread-safe, ready for concurrent applications.

### Dependencies

Tiny-Bean relies on two key libraries:

- **`io.github.classgraph`**: A fast and powerful classpath scanner used to discover bean definitions.
- **`org.projectlombok:lombok`**: A compile-time library to reduce boilerplate code within the framework itself.

---

## Getting Started

### 1. Add the JitPack Repository

This project is distributed via [JitPack](https://jitpack.io). To use it, you must add the JitPack repository to your
build file.

For Maven, add this to your `pom.xml`:

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Add the Tiny-Bean Dependency

Once the repository is added, you can include Tiny-Bean in your project. Replace `1.5.0` with the
desired [release tag](https://github.com/Piloxal-po/tiny-bean/tags).

```xml

<dependency>
    <groupId>com.github.Piloxal-po</groupId>
    <artifactId>tiny-bean</artifactId>
    <version>1.5.0</version>
</dependency>
```

### 3. Create the Main Application Class

Your application needs a main entry point. Create a class and annotate it with `@Application`. This annotation tells
Tiny-Bean where to start scanning for components.

```java
import com.github.oxal.annotation.Application;
import com.github.oxal.runner.ApplicationRunner;

// The @Application annotation marks this as the root of the context.
// It will scan all classes within the "com.myapp" package.
@Application(packages = "com.myapp")
public class MyApplication {

    public static void main(String[] args) {
        // 1. Load the context
        ApplicationRunner.loadContext(MyApplication.class);

        // 2. Start using your beans!
        MyService myService = ApplicationRunner.loadBean(MyService.class);
        myService.doSomething();
    }
}
```

---

## Usage Examples

### Defining Beans with `@Bean`

There are two ways to declare a bean: by annotating a class or a method.

#### Class-Based Beans

Simply add the `@Bean` annotation to a class. Tiny-Bean will automatically detect it and make it available for
injection.

```java
package com.myapp.service;

import com.github.oxal.annotation.Bean;

@Bean
public class MyService {
    // ...
}
```

#### Method-Based Beans

You can also define beans within a configuration class. This is useful for integrating third-party classes or when
complex setup logic is needed.

```java
package com.myapp.config;

import com.github.oxal.annotation.Bean;

@Bean
public class AppConfig {
    @Bean("welcomeMessage")
    public String welcomeMessage() {
        return "Welcome to the application!";
    }
}
```

### Resolving Ambiguity with `@Qualifier` and `@Primary`

When multiple beans of the same type exist, the container needs help deciding which one to inject. You have two main
options: `@Qualifier` and `@Primary`.

#### Using `@Qualifier` for Specificity

Use `@Qualifier` when you need to select a specific bean by its name. This is the most precise way to resolve ambiguity.

```java
import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;

@Bean
public class GreetingService {
    private final String message;

    // Injects the bean named "welcomeMessage"
    public GreetingService(@Qualifier("welcomeMessage") String message) {
        this.message = message;
    }
    // ...
}
```

#### Using `@Primary` for Defaults

Use `@Primary` to mark one bean as the default choice when multiple candidates are available. This is useful when you
have a common or preferred implementation.

Imagine you have an interface `NotificationService` with two implementations:

```java
public interface NotificationService {
    void send(String message);
}

@Bean("emailNotification")
public class EmailNotificationService implements NotificationService {
    // ...
}

@Primary // <-- Marks this as the default implementation
@Bean("smsNotification")
public class SmsNotificationService implements NotificationService {
    // ...
}
```

Now, when another bean requests a `NotificationService` without specifying a name, Tiny-Bean will automatically inject
the primary one (`SmsNotificationService`).

```java
@Bean
public class UserAccountManager {
    private final NotificationService notificationService;

    // No @Qualifier needed! SmsNotificationService will be injected.
    public UserAccountManager(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

> **Note:** If you declare more than one `@Primary` bean for the same type, the application context will fail to load,
> as it creates an unresolvable ambiguity.

### Using Scopes

Tiny-Bean supports two scopes: `SINGLETON` (default) and `PROTOTYPE`.

#### Singleton Scope (Default)

A single instance of the bean is created and shared across the entire application.

```java
// MyService is a singleton by default
MyService service1 = ApplicationRunner.loadBean(MyService.class);
MyService service2 = ApplicationRunner.loadBean(MyService.class);

// This will be true
assert service1 ==service2;
```

#### Prototype Scope

A new instance of the bean is created every time it is requested.

```java
package com.myapp.service;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;

@Bean(scope = ScopeType.PROTOTYPE)
public class TaskProcessor {
    // ...
}
```

```java
TaskProcessor processor1 = ApplicationRunner.loadBean(TaskProcessor.class);
TaskProcessor processor2 = ApplicationRunner.loadBean(TaskProcessor.class);

// This will be true, as a new instance is created each time
assert processor1 !=processor2;
```

---

## Advanced Features

### Stereotype Annotations

To better organize your code, you can create your own "stereotype" annotations that act as specialized aliases for
`@Bean`. To do this, simply create a new annotation and meta-annotate it with `@Bean`.

**1. Create your custom annotation:**

To pass through properties like `value` (for the bean name) or `scope`, you must redeclare them in your custom
annotation.


```java
import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.ScopeType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bean // <-- Meta-annotation
public @interface Service {
    // Redeclare attributes to allow customization
    String value() default Bean.DEFAULT;

    ScopeType scope() default ScopeType.SINGLETON;
}
```

**2. Use your stereotype:**

You can now use `@Service` instead of `@Bean` for a more descriptive component model.

```java

@Service("mySpecialService")
public class MySpecialService {
    // ...
}
```

This bean will be registered with the name `mySpecialService` and a singleton scope.

### Lifecycle Callbacks

You can hook into the application startup lifecycle using `@BeforeContextLoad` and `@AfterContextLoad`. This is useful
for setting up resources, running initialization logic, or logging.

Methods are executed in order based on the `order()` property (lower values run first).

#### `@BeforeContextLoad`

These methods are executed **before** the IoC container is created. They are ideal for tasks that need to happen before
any beans are defined. The method can have 0 parameters or 1 parameter of type `io.github.classgraph.ScanResult`.

```java
public class SystemInitializer {
    @BeforeContextLoad(order = 1)
    public void setupSystemProperties(ScanResult scanResult) {
        System.out.println("Found " + scanResult.getAllClasses().size() + " classes during scan.");
        System.setProperty("my.app.started", "true");
    }
}
```

#### `@AfterContextLoad`

These methods are executed **after** the IoC container has been created and all bean definitions have been scanned. The
method's parameters are **injected with dependencies** from the context. You can ask for any bean, the `Context` itself,
or the `ScanResult`.

```java
public class AppInitializer {
    @AfterContextLoad(order = 10)
    public void onStartup(MyService myService, Context context) {
        System.out.println("Application context has been loaded with " + context.getBeanDefinitions().size() + " beans.");
        myService.startBackgroundTask();
    }
}
```

### Extending the Classpath Scan with ServiceLoader

For modular applications, you may need to scan packages from different modules or libraries. Tiny-Bean supports this
using Java's built-in `ServiceLoader` mechanism.

By creating your own `PackageProvider`, you can instruct the `ApplicationRunner` to include additional packages in its
classpath scan.

**1. Implement the `PackageProvider` Interface:**

Create a class that implements `com.github.oxal.provider.PackageProvider`.

```java
package com.mycompany.externalmodule;

import com.github.oxal.provider.PackageProvider;

public class MyModulePackageProvider implements PackageProvider {
    @Override
    public String[] getPackages() {
        // Return the list of packages to be scanned in this module
        return new String[]{"com.mycompany.externalmodule.beans"};
    }
}
```

**2. Register the Implementation:**

Create a service provider configuration file in your module's `resources` directory.

**File Path:** `src/main/resources/META-INF/services/com.github.oxal.provider.PackageProvider`

**File Content:**

```
# Fully qualified name of your implementation
com.mycompany.externalmodule.MyModulePackageProvider
```

**3. Let Tiny-Bean Do the Rest:**

When `ApplicationRunner.loadContext()` is called, it will automatically:
- Discover all `PackageProvider` implementations registered on the classpath.
- Add the packages they provide to the list of packages to be scanned.
- Find and register any beans (e.g., `@Bean`, `@Service`) defined in those packages.

This allows you to create decoupled modules that automatically integrate themselves into the main application's IoC
container.

---

## How It Works

1. **`ApplicationRunner.loadContext(Main.class)`:**
    - It gathers packages to scan from `@Application`, and from all `PackageProvider` services.
    - A single classpath scan is performed on the combined list of packages.
    - `@BeforeContextLoad` callbacks are found and executed immediately.
    - The `Context` object is created.
    - The `ScanResult` is registered as a bean.
    - Bean definitions (`@Bean` or stereotypes) and `@AfterContextLoad` callbacks are registered in the context.
    - `@AfterContextLoad` callbacks are executed, with their dependencies injected.
2. **`ApplicationRunner.loadBean(MyClass.class)`:**
    - The framework finds the bean's definition.
    - If the bean is a singleton and already exists in the cache, it's returned.
    - Otherwise, it recursively resolves, loads, and creates any dependencies needed.
    - It creates the final instance.
    - If the bean is a singleton, the instance is cached for all future requests.
    - If the bean is a prototype, a new instance is created and returned without caching.

---

## License

This project is licensed under the MIT License.

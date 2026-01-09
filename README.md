# Tiny-Bean: A Lightweight Dependency Injection Framework

**Tiny-Bean** is a minimalist, annotation-driven Inversion of Control (IoC) container for Java. Inspired by the core principles of frameworks like Spring, it provides a simple yet powerful way to manage your application's components and their dependencies.

It is designed to be easy to understand and lightweight, with minimal, carefully chosen dependencies.

---

## Core Features

- **Annotation-Driven:** Configure your application using simple annotations like `@Application`, `@Bean`, and `@Qualifier`.
- **Classpath Scanning:** Automatically discovers and registers your beans from specified packages.
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

This project is distributed via [JitPack](https://jitpack.io). To use it, you must add the JitPack repository to your build file.

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

Once the repository is added, you can include Tiny-Bean in your project. Replace `v1.0.0` with the desired [release tag](https://github.com/oxal/tiny-bean/tags).

```xml
<dependency>
    <groupId>com.github.oxal</groupId>
    <artifactId>tiny-bean</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Create the Main Application Class

Your application needs a main entry point. Create a class and annotate it with `@Application`. This annotation tells Tiny-Bean where to start scanning for components.

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

### Defining Beans

There are two ways to declare a bean: by annotating a class or a method.

#### Class-Based Beans

Simply add the `@Bean` annotation to a class. Tiny-Bean will automatically detect it and make it available for injection.

```java
package com.myapp.service;

import com.github.oxal.annotation.Bean;

@Bean
public class MyService {
    
    private final MyRepository myRepository;

    // Dependencies are automatically injected via the constructor
    public MyService(MyRepository myRepository) {
        this.myRepository = myRepository;
    }

    public void doSomething() {
        System.out.println("Doing something with data: " + myRepository.getData());
    }
}
```

```java
package com.myapp.repository;

import com.github.oxal.annotation.Bean;

@Bean
public class MyRepository {
    public String getData() {
        return "Hello, Tiny-Bean!";
    }
}
```

#### Method-Based Beans

You can also define beans within a configuration class. This is useful for integrating third-party classes or when complex setup logic is needed.

```java
package com.myapp.config;

import com.github.oxal.annotation.Bean;

// A class that holds bean definitions
@Bean
public class AppConfig {

    // This method produces a String bean named "welcomeMessage"
    @Bean("welcomeMessage")
    public String welcomeMessage() {
        return "Welcome to the application!";
    }

    // This method produces another String bean with a different name
    @Bean("goodbyeMessage")
    public String goodbyeMessage() {
        return "Thanks for using the application!";
    }
}
```

### Injecting Dependencies with `@Qualifier`

When you have multiple beans of the same type, you can use `@Qualifier` to specify which one to inject.

```java
package com.myapp.service;

import com.github.oxal.annotation.Bean;
import com.github.oxal.annotation.Qualifier;

@Bean
public class GreetingService {

    private final String message;

    // Inject the bean named "welcomeMessage"
    public GreetingService(@Qualifier("welcomeMessage") String message) {
        this.message = message;
    }

    public void greet() {
        System.out.println(message);
    }
}
```

### Using Scopes

Tiny-Bean supports two scopes: `SINGLETON` (default) and `PROTOTYPE`.

#### Singleton Scope (Default)

A single instance of the bean is created and shared across the entire application.

```java
// MyService is a singleton by default
MyService service1 = ApplicationRunner.loadBean(MyService.class);
MyService service2 = ApplicationRunner.loadBean(MyService.class);

// This will be true
assert service1 == service2;
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
assert processor1 != processor2;
```

---

## How It Works

1.  **`ApplicationRunner.loadContext(Main.class)`:** You initialize the context by pointing to your `@Application` class.
2.  **Scanning:** The framework scans the packages specified in `@Application` for any classes or methods annotated with `@Bean`. It stores these "bean definitions" without creating any objects.
3.  **`ApplicationRunner.loadBean(MyClass.class)`:** When you request a bean for the first time:
    - The framework finds its definition.
    - It recursively resolves, loads, and creates any dependencies needed by the bean's constructor.
    - It creates the final instance.
    - If the bean is a **singleton**, the instance is cached for all future requests.
    - If the bean is a **prototype**, a new instance is created and returned without caching.

---

## License

This project is licensed under the MIT License.

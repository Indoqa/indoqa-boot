# Spring - Manage your components

## Option 1: Spring component scan

The [AbstractIndoqaBootApplication](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/application/AbstractIndoqaBootApplication.html) provides the method `getComponentScanBasePackages`. Return all packages that should be included in the Spring component scan. The Spring documentation explains [how to annotate](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-classpath-scanning) your classes so that they are registered as components. Consider using [JSR 330 standard annotations](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-standard-annotations) to reduce the dependencies on Spring in your components.

## Option 2: Register a Spring configuration

The Indoqa-boot [StartupLifecycle](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/application/StartupLifecycle.html) gives access to the Spring application context.

```java
@Override
public void willRefreshSpringContext(AnnotationConfigApplicationContext context) {
    context.register(SomeSpringComponentConfiguration.class);
}
```

See [Initialization in your Java main method](./initialization-java-main.md) for information how to make use of the StartupLifecycle.

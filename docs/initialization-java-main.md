# Initialization in your Java main method

The entry point to Indoqa-Boot is the [AbstractIndoqaBootApplication](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/application/AbstractIndoqaBootApplication.html). Extend this class and implement the main method which has to invoke Indoqa-Boot:

```java
public class Application extends AbstractIndoqaBootApplication {

    public static void main(String[] args) {
        new Application().invoke();
    }

    @Override
    protected String getApplicationName() {
        return "My Application";
    }
}
```
Additionally to `getApplicationName` there are more template methods that you can override: `checkLoggerInitialization`, `getAsciiLogoPath`, `getComponentScanBasePackages`, `getJsonTransformerClass`, `getVersionProvider` and `isDevEnvironment`. See [AbstractIndoqaBootApplication](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/application/AbstractIndoqaBootApplication.html) for further details.

## Custom StartupLifecycle implementation

If you want to hook into the initialization of Indoqa-Boot, extend [AbstractStartupLifecycle](https://static.javadoc.io/com.indoqa/indoqa-boot/0.8.0/com/indoqa/boot/application/AbstractStartupLifecycle.html) and pass it to the `invoke` method:

```java
public class Application extends AbstractIndoqaBootApplication {

    public static void main(String[] args) {
        new Application().invoke(new CustomStartupLifecycle());
    }
}
```

The interface [StartupLifecycle](https://static.javadoc.io/com.indoqa/indoqa-boot/0.8.0/com/indoqa/boot/application/StartupLifecycle.html) explains the lifecycle methods in detail.

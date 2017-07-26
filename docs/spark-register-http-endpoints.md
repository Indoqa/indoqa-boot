# Register your HTTP endpoints

Spark can be used as it is. This means that everything described in the [Spark documentation](http://sparkjava.com/documentation) can be applied.

Since the idea of Indoqa-Boot is to use Spring managed components to structure Java services, you need those services being injected into your Spark endpoint classes. Our recommendation is that the classes that register Spark endpoints are also Spring components and get the required services injected:

```java
public class MyResource {

    @Inject
    private MyService myService;

    @PostConstruct
    public void mount() {
        Spark.get("/test", (req, res) -> myService.execute()));
    }
}
```

Annotating a Spring component method with `@PostConstruct` ensures that this happens after all dependencies are injected.

In the case that you want to make use of Spark configurations that have to be executed before any route is mapped (e.g. registering static files), make sure that this happens before. See [Custom StartupLifecycle implementation](./initialization-java-main.md) for details.

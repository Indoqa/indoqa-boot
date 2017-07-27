# Configuration: Provide properties for run profiles, ports etc.

Indoqa-Boot allows setting several Spring properties to adapt Indoqa-Boot to your environment.

## Spring properties

The Spring properties can be set either as system properties or via a Spring property source.

### port

The property `port` configures the port of the default Spark service which should be used to expose all HTTP endpoints that are usually exposed to the users of the service. The default port is `3456`.

### admin.port

Indoqa-Boot usually start a second Spark service that exposes all administrative HTTP endpoints like `/status-info` or `/shutdown`. The default admin port is `34567`.

### admin.separate-service

If you want to expose all administrative HTTP endpoints via the default Spark service, set this property to true. But be aware that in this case that internal information will be exposed and the `/shutdown` resource can be invoked.

## System properties

The following properties have to be provided as system properties.

### log-path

The property `log-path` has to point to the directory in your file system and should be used in your Log4j logging configuratin.

Note that setting the `log-path` property is mandatory by default. If you want to avoid this behavior, override the method `checkLoggerInitialization` of [AbstractJsonResourcesBase](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/json/resources/AbstractJsonResourcesBase.html) and return `false`

### spring.profiles.active

Indoqa-Boot initializes a Spring application context. During this initialization process Spring checks `spring.profiles.active` which you can use to explicitly set the Spring profile that you want to use.

Note that Indoqa-Boot automatically sets either the profile `prod` or `dev` by calling the method `isDevEnvironment` provided by [AbstractJsonResourcesBase](https://www.javadoc.io/page/com.indoqa/indoqa-boot/latest/com/indoqa/boot/json/resources/AbstractJsonResourcesBase.html). If you override the system property `spring.profiles.active` this check is omitted.

Please refer to the [Spring documentation](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-definition-profiles-enable) for further details.

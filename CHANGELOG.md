# v0.15.0 | TBA
* register multiple React applications in FrontendResource with test functions to select one at request time

# v0.14.0 | 2019-06-25
* only send HTML response when the request was a GET
* introduce ResponseEnhancements to dynamically adapt the HML produced by AbstractCreateReactAppResourceBase
* Log4j2LoggingResource (admin port)

# v0.13.0 | 2019-03-27
* add AbstractCreateReactAppResourceBase for serving React applications created by CreateReactApp

# v0.12.0 | 2019-02-13
* fix npe if there are no javascript assets
* fully support webpack code splitting
* remove classpath scanning for webpack assets completely

# v0.11.0 | 2018-10-12
* use asset-manifest.json if available (to avoid classpath scanning operations
* set response header 'Indoqa-Boot-Health' with the result of the health check
* increase shutdown check period
* HealthCheckResources: catch exceptions in timeThread;
* HealthCheckResources: no fixedRate scheduling

# v0.10.0 | 2018-04-19
* Java9 support
* SystemInfo: consistent property names
* health status by head request via business rest service port
* add CompositeHealthIndicator
* Fix health check json output

# v0.9.0 | 2017-12-17
* introduce health checking (via admin port)
* introduce metrics info (via admin port)
* support heap dumps (via admin port)
* support thread dumps (via admin port)
* error mapping
* expose git.properties if available as system-info
* remove spring-web dependency
* add HtmlResponseModifier to optionally modify html response headers
* use HtmlBuilder to create snippets in frontend resource

# v0.8.0 | 2017-06-22
* Application: default implementation for isDevEnvironment
* expose admin-port via SystemInfo
* fix bug if admin port is null

# v0.7.0 | 2017-06-08
* ReactHtmlBulder: allow setting the jsonTransformer
* refactor react html integration

# v0.6.0 | 2017-02-15
* warn if the HtmlEscapingJacksonTransformer is not used
* shut down if the default port OR the admin port is in use
* register the shutdown resource as admin service if available otherwise as default service
* fix XSS vulnerability if the JSON string is serialized into a <script> tag
* changed the mount of HTML resources from a route to an "after" filter

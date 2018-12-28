# v0.12.0 | TBD
* fix npe if there are no javascript assets
* fully support webpack code splitting
* remove classpath scanning for webpack assets

# v0.11.0 | 2018-10-12
* use asset-manifest.json if avaiable (to avoid classpath scanning operations
* set response header 'Indoqa-Boot-Health' with the result of the health check
* increase shutdown check period
* HealthCheckResources: catch exceptions in timeThread; no fixedRate scheduling

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
* expose git.properties if avaible as system-ifno
* remove spring-web dependency
* add HtmlResponseModifier to optionally modiy html response headers
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
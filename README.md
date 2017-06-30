# Indoqa Boot
[![Javadocs](https://www.javadoc.io/badge/com.indoqa/indoqa-boot.svg)](https://www.javadoc.io/doc/com.indoqa/indoqa-boot)

## Getting started

The best way to bootstrap an indoqa-boot application is using the [indoqa-quickstart-boot](https://github.com/Indoqa/indoqa-quickstart/tree/master/indoqa-quickstart-boot) Maven archetype:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.indoqa.quickstart \
  -DarchetypeArtifactId=quickstart-boot-archetype \
  -DarchetypeVersion=0.8.0.1
```

## Motivation and history

In 2015 [Indoqa](https://www.indoqa.com) started to look for alternatives to traditional Java web application frameworks like JSF, Apache Wicket or Vaadin. During this journey we decided to give up Java for building web frontends and switch to Javascript. This had brought us to the question how we wanted to implement the frontend-backend application. One alternative would have been to use node.js for the frontend-backend implementation but in our opinion the Java platform is still years ahead in terms of stability and available libraries and tools for the development of server side applications.

The first obvious choice would have been using [Spring Boot](https://projects.spring.io/spring-boot/) but we were overwhelmed with annotations and the learning curve. We only needed a simple framework to provide RESTful resources and a JSON transformer. We came across [Javaspark](http://sparkjava.com/) and were immediately intrigued by its simplicity. The integration with [Jackson](https://github.com/FasterXML/jackson) was quickly done and this was the basis for the first internal release of Indoqa Boot. The third ingredient was the [Spring Framework](https://projects.spring.io/spring-framework/), which we use as dependency injection framework since its alpha days and helps us to structure our code.

## Goals

 * Build on stable and widely adapted open source software with active communities

 * Simple project setup, cover HTTP endpoints, JSON, dependency injection and logging.

 * Seamless integration of [React](https://facebook.github.io/react/)/[Redux](http://redux.js.org/) single page applications based on [indoqa-react-app](https://github.com/Indoqa/indoqa-react-app) and [indoqa-webpack](https://github.com/Indoqa/indoqa-webpack).

 * Reduce the usage of annotations to a minimum. We are not strictly against annotations (we use them e.g. for dependency injection or the mapping of Java objects to JSON) but for the most other use cases (e.g. configuration of Indoqa Boot, creation of HTTP endpoints) we prefer to write plain Java code.

 * Support proxying of web services

 * Creation of an as small as possible runnable Java archive (currently 11.5 megabytes) to make the distribution and the deployment of the application simple by not relying on (bloated) Java application servers

 * Small memory footprint

 * Fast startup times (< 1 second) and short development cycles

 * Central dependency management using a Maven bill of material (BOM) to centrally manage library updates

## Technologies

Indoqa-Boot is built upon following open source libraries and frameworks:

 * [Javaspark](http://sparkjava.com/) which comes with [Jetty](http://www.eclipse.org/jetty/)
 * [Jackson](https://github.com/FasterXML/jackson)
 * [Spring Framework](https://projects.spring.io/spring-framework/)
 * [log4j2](https://logging.apache.org/log4j/2.x/)

 Additionally we use following Indoqa open source libraries:

 * [indoqa-http-proxy](https://github.com/Indoqa/http-proxy) to proxy pass other web services without having to deal with CORS
 * [system-test-tools](https://github.com/Indoqa/system-test-tools) for a DSL to setup and run integration tests against HTTP endpoints
 * [jar-in-jar](https://github.com/Indoqa/jar-in-jar) to create runnable JAR files
 * [indoqa-boot-bom](https://github.com/Indoqa/indoqa-boot-bom/blob/master/pom.xml) to manage Indoqa Boot relevant dependencies

## Usage

 * [Initialization in your Java main method]()
 * [Spring: Manage your components](./docs/spring-manage-your-components.md)
 * [Spark: Register your HTTP endpoints]()
 * [Jackson: Dealing with Json]()
 * [Configuration: Provide properties for run profiles, ports etc.]()
 * [React/Redux: Integrate with a Javascript single page application]()
 * [log4j2: Configure logging]()
 * [Monitoring: Health checks and system info]()
 * [Maven: Configure your Maven build and produce deployment artifacts]()
 * [Hot-reload Java]()
 * [Integration tests]()

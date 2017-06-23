# Indoqa Boot
[![Javadocs](https://www.javadoc.io/badge/com.indoqa/indoqa-boot.svg)](https://www.javadoc.io/doc/com.indoqa/indoqa-boot)

## Motivation and history

In 2015 [Indoqa](https://www.indoqa.com) has started to look for alternatives to traditional Java web application frameworks. During this journey we decided to give up Java and switch to Javascript for frontends. This had brought us to the question how we wanted to implement the frontend-backend application. One alternative would have been to use node.js for the frontend-backend implementation but in our opinion the Java platform is still years ahead in terms of stability and available libraries and tools.

The first obvious choice would have been using [Spring Boot](https://projects.spring.io/spring-boot/) but we where overwhelmed with annotations and the learning curve. We only needed a simple framework to provide RESTful resources and a JSON transformer. We came across [Javaspark](http://sparkjava.com/) and where immediately intrigued by its simplicity. The integration with [Jackson](https://github.com/FasterXML/jackson) was quickly done which was the basis for the first internal release of Indoqa Boot. The third ingredient was [Spring Framework](https://projects.spring.io/spring-framework/), which we use since its alpha days, which helps us to structure our code.

## Goals

 * Reduce the usage of annotations to a minimum. We are not strictly against annotations (we use them e.g. for dependency injection and mapping Java objects to JSON) but for the most other use cases we prefer to write Java code.

 * Make the integration of [React](https://facebook.github.io/react/)/[Redux](http://redux.js.org/) single page applications simple.

 * Creation of runnable Java archives to make the distribution and the deployment of the application simple.

## Technology

Indoqa-Boot is built upon following libraries and frameworks:

 * [Javaspark](http://sparkjava.com/) which comes with [Jetty](http://www.eclipse.org/jetty/)
 * [Jackson](https://github.com/FasterXML/jackson)
 * [Spring Framework](https://projects.spring.io/spring-framework/)
 * [log4j2](https://logging.apache.org/log4j/2.x/)

## Usage

 * [Getting started with a Maven archetype](./docs/getting-started-with-a-maven-archetype.md)
 * [Initialization in your Java main method]()
 * [Spring: Manage your components](./docs/spring-manage-your-components.md)
 * [Spark: Register your HTTP endpoints]()
 * [Jackson: Dealing with Json]()
 * [React.js: Integrate with a Javascript single page application]()
 * [log4j2: Configure logging]()
 * [Monitoring: Health checks and system info]()
 * [Maven: Configure your Maven build and deployment artifacts]()
 * [Hot-reload Java]()
 * [Integration tests]()
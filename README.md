WildFly Discovery
============

This project facilitates the location of services based on abstract descriptors. It consists of a service type and a filter specification.
Current maven coordinates of the project is defined as
`<groupId>org.wildfly.discovery</groupId>
 <artifactId>wildfly-discovery-client</artifactId>
 <version>1.2.2.Final-SNAPSHOT</version>`

Each discovery API instance is associated with discovery providers which are able to
provide answers to discovery queries.

Building
-------------------

Prerequisites:

* JDK 11 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`

To build with your own Maven installation:

> mvn install

Contributing
------------------

Find issues here: https://issues.jboss.org/browse/WFDISC

Relevant Documentation
------------------

http://docs.wildfly.org/14/Client_Guide.html
# Report Portal Listener for TestNG tests
A TestNG reporter that uploads the results to a ReportPortal server.

> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's and client's names, and their versions
> after a successful launch start. This information might help us to improve both ReportPortal backend and client sides. It is used by the
> ReportPortal team only and is not supposed for sharing with 3rd parties.

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/agent-java-testng.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.epam.reportportal/agent-java-testng)
[![CI Build](https://github.com/reportportal/agent-java-testNG/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/agent-java-testNG/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/agent-java-testNG/branch/develop/graph/badge.svg?token=CshHrWt7sS)](https://codecov.io/gh/reportportal/agent-java-testNG)
[![Join Slack chat!](https://slack.epmrpp.reportportal.io/badge.svg)](https://slack.epmrpp.reportportal.io/)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)

The latest version: $LATEST_VERSION. Please use `Maven Central` link above to get the agent.
**For TestNG version [7.1.0](https://central.sonatype.com/artifact/org.testng/testng/7.1.0) and higher**

## Overview: How to Add ReportPortal Logging to Your Project

To start using Report Portal with TestNG framework please do the following steps:

1. [Configuration](#configuration)
  * Create/update the `reportportal.properties` configuration file
  * Build system configuration
  * Add Listener
2. [Logging configuration](#logging)
  * Loggers and their types
3. [Running tests](#running-tests)
  * Build system commands
4. [Custom use examples](#customization)


## Configuration
### 'reportportal.properties' configuration file

To start using Report Portal you need to create a file named `reportportal.properties` in your Java project in a source
folder `src/main/resources` or `src/test/resources` (depending on where your tests are located):

**reportportal.properties**

```
rp.endpoint = http://localhost:8080
rp.api.key = e0e541d8-b1cd-426a-ae18-b771173c545a
rp.launch = TestNG Tests
rp.project = default_personal
```

**Property description**

* `rp.endpoint` - the URL for the report portal server (actual link).
* `rp.api.key` - an access token for Report Portal which is used for user identification. It can be found on your report
  portal user profile page.
* `rp.project` - a project ID on which the agent will report test launches. Must be set to one of your assigned
  projects.
* `rp.launch` - a user-selected identifier of test launches.


The full list of supported properties is located here in client-java library documentation (a common library for all
Java agents): https://github.com/reportportal/client-java

## Build system configuration

### Maven

If your project is Maven-based the first thing, which you should do, is to add dependencies to `pom.xml` file:
```xml
<project>
  <!-- project declaration omitted -->
  
  <dependency>
    <groupId>com.epam.reportportal</groupId>
    <artifactId>agent-java-testng</artifactId>
    <version>$LATEST_VERSION</version>
    <scope>test</scope>
  </dependency>

  <!-- build config omitted -->
</project>
```
You are free to use you own version of TestNG, but not earlier than 7.1.0. If you leave just Agent dependency it will
be still OK, it will use transitive TestNG version.

### Gradle

For Gradle-based projects please update dependencies section in `build.gradle` file:
```groovy
dependencies {
    testImplementation 'com.epam.reportportal:agent-java-testng:$LATEST_VERSION'
}
```

## Listener configuration
There are many ways to configure a listener in TestNG, but the most elegant and recommended way is to use a
`ServiceLoader` file. Here is how you can do that:

1. Create folders **_/META-INF/services_** in **_resources_** folder (`src/main/resources` or `src/test/resources`)
2. Put there a file named **_org.testng.ITestNGListener_**
3. Put a default implementation reference as a single row into the file: **_com.epam.reportportal.testng.ReportPortalTestNGListener_**

Example:
__/META-INF/services/org.testng.ITestNGListener__
```none
com.epam.reportportal.testng.ReportPortalTestNGListener
```

That's it! You are all set.

## Logging

Report Portal provides its own logger implementations for major logging frameworks like *Log4j* and *Logback*. It also
provides additional formatting features for popular client and test libraries like: *Selenide*, *Apache HttpComponents*,
*Rest Assured*, etc.

Here is the list of supported loggers and setup documentation links.

**Logging frameworks:**

| **Library name**       | **Documentation link**                                      |
|------------------------|-------------------------------------------------------------|
| Log4j                  | https://github.com/reportportal/logger-java-log4j           |
| Logback                | https://github.com/reportportal/logger-java-logback         |

**HTTP clients:**

| **Library name**       | **Documentation link**                                      |
|------------------------|-------------------------------------------------------------|
| OkHttp3                | https://github.com/reportportal/logger-java-okhttp3         |
| Apache HttpComponents  | https://github.com/reportportal/logger-java-httpcomponents  |

**Test frameworks:**

| **Library name** | **Documentation link**                                     |
|------------------|------------------------------------------------------------|
| Selenide         | https://github.com/reportportal/logger-java-selenide       |
| Rest Assured     | https://github.com/reportportal/logger-java-rest-assured   |

## Running tests

We are set. To run set we just need to execute corresponding command in our build system.

#### Maven

`mvn test` or `mvnw test` if you are using Maven wrapper

#### Gradle

`gradle test` or `gradlew test` if you are using Gradle wrapper

## Customization

### Code example How to overload params in run-time

As a sample you can use code for **Override UUID** in run-time
```java
	public static class MyListener extends BaseTestNGListener {
		public MyListener() {
			super(new ParamOverrideTestNgService());
		}
	}

	public static class ParamOverrideTestNgService extends TestNGService {
		public ParamOverrideTestNgService() {
			super(getLaunchOverriddenProperties());
		}

		private static Supplier<Launch> getLaunchOverriddenProperties() {
			ListenerParameters parameters = new ListenerParameters(PropertiesLoader.load());
			parameters.setApiKey("my crazy uuid");
			ReportPortal reportPortal = ReportPortal.builder().withParameters(parameters).build();
			StartLaunchRQ rq = buildStartLaunch(reportPortal.getParameters());
			return new Supplier<Launch>() {
				@Override
				public Launch get() {
					return reportPortal.newLaunch(rq);
				}
			};
		}

		private static StartLaunchRQ buildStartLaunch(ListenerParameters parameters) {
			StartLaunchRQ rq = new StartLaunchRQ();
			rq.setName(parameters.getLaunchName());
			rq.setStartTime(Calendar.getInstance().getTime());
			rq.setAttributes(parameters.getAttributes());
			rq.setMode(parameters.getLaunchRunningMode());
			if (!Strings.isNullOrEmpty(parameters.getDescription())) {
				rq.setDescription(parameters.getDescription());
			}

			return rq;
		}
	}
```

### Example repository

There are two modules under Example project which represent agent usage with Lo4j and Logback loggers:
* https://github.com/reportportal/examples-java/tree/master/example-testng-log4j
* https://github.com/reportportal/examples-java/tree/master/example-testng-logback

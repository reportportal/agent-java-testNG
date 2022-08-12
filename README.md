# agent-java-testNG
A TestNG reporter that uploads the results to a ReportPortal server.

> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's and client's names, and their versions
> after a successful launch start. This information might help us to improve both ReportPortal backend and client sides. It is used by the
> ReportPortal team only and is not supposed for sharing with 3rd parties.

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/agent-java-testng.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22agent-java-testng%22)
[![CI Build](https://github.com/reportportal/agent-java-testNG/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/agent-java-testNG/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/agent-java-testNG/branch/develop/graph/badge.svg?token=CshHrWt7sS)](https://codecov.io/gh/reportportal/agent-java-testNG)
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)
---
- [Objects interrelation TestNG - ReportPortal](https://github.com/reportportal/agent-java-testNG#objects-interrelation-testng---reportportal)
- [Dependencies](https://github.com/reportportal/agent-java-testNG#dependencies)
- [Install listener](https://github.com/reportportal/agent-java-testNG#install-listener)
  - [Listener class](https://github.com/reportportal/agent-java-testNG#listener-class)
  - [Listener parameters](https://github.com/reportportal/agent-java-testNG/#listener-parameters)
  - [Maven Surefire plugin](https://github.com/reportportal/agent-java-testNG#maven-surefire-plugin)
  - [Specify listener in testng.xml](https://github.com/reportportal/agent-java-testNG#specify-listener-in-testngxml)
  - [Custom runner](https://github.com/reportportal/agent-java-testNG#custom-runner)
  - [Using command line](https://github.com/reportportal/agent-java-testNG#using-command-line)
  - [Using \@Listeners annotation](https://github.com/reportportal/agent-java-testNG#using-listeners-annotation)
  - [Using ServiceLoader](https://github.com/reportportal/agent-java-testNG#using-serviceloader)
- [Code example How to overload params in run-time](https://github.com/reportportal/agent-java-testNG#code-example-how-to-overload-params-in-run-time)
- [Example and step-by-step instruction with logback](https://github.com/reportportal/examples-java/tree/master/example-testng-logback)
- [Example and step-by-step instruction with Log4j](https://github.com/reportportal/examples-java/tree/master/example-testng-log4j)
---

**[TestNG](http://testng.org)** provides support for attaching custom listeners, reporters, annotation transformers and method interceptors to your tests.
Handling events

TestNG agent can handle next events:

-   Start launch
-   Finish launch
-   Start suite
-   Finish suite
-   Start test
-   Finish test
-   Start test step
-   Successful finish of test step
-   Fail of test step
-   Skip of test step
-   Start configuration (All «before» and «after» methods)
-   Fail of configuration
-   Successful finish of configuration
-   Skip configuration

## Objects interrelation TestNG - ReportPortal

| **TestNG object**    | **ReportPortal object**       |
|----------------------|-------------------------------|
| LAUNCH               |LAUNCH                         |
| BEFORE_SUITE         |TestItem (type = BEFORE_SUITE) |
| BEFORE_GROUPS        |TestItem (type = BEFORE_GROUPS)|
| SUITE                |TestItem (type = SUITE)        |
| BEFORE_TEST          |TestItem (type = BEFORE_TEST)  |
| TEST                 |TestItem (type = TEST)         |
| BEFORE_CLASS         |TestItem (type = BEFORE_CLASS) |
| CLASS                |Not in structure. Avoid it     |
| BEFORE_METHOD        |TestItem (type = BEFORE_METHOD)|
| METHOD               |TestItem (type = STEP)         |
| AFTER_METHOD         |TestItem (type = AFTER_METHOD) |
| AFTER_CLASS          |TestItem (type = AFTER_CLASS)  |
| AFTER_TEST           |TestItem (type = AFTER_TEST)   |
| AFTER_SUITE          |TestItem (type = AFTER_SUITE)  |
| AFTER_GROUPS         |TestItem (type = AFTER_GROUPS) |

TestItem – report portal specified object for representing:  suite, test, method objects in different test systems. Used as tree structure and can be recursively placed inside himself.

## Dependencies
> Minimum supported TestNG version: [7.1.0](https://search.maven.org/artifact/org.testng/testng/7.1.0/jar)


Add to `POM.xml`

**dependency**

```xml
<dependency>
  <groupId>com.epam.reportportal</groupId>
  <artifactId>agent-java-testng</artifactId>
  <version>5.1.3</version>
</dependency>
<!-- TODO Leave only one dependency, depends on what logger you use: -->
<dependency>
  <groupId>com.epam.reportportal</groupId>
  <artifactId>logger-java-logback</artifactId>
  <version>5.1.1</version>
</dependency>
<dependency>
  <groupId>com.epam.reportportal</groupId>
  <artifactId>logger-java-log4j</artifactId>
  <version>5.1.4</version>
</dependency>
```

## Install listener

Download package [here](<https://search.maven.org/search?q=g:%22com.epam.reportportal%22%20AND%20a:%22agent-java-testng%22>).
Choose latest version.

By default, TestNG attaches a few basic listeners to generate HTML and XML
reports. For reporting TestNG test events (ie start of test, successful finish
of test, test fail) to ReportPortal user should add ReportPortal TestNg
listener to run and configure input parameters. 

### Listener parameters
Description of listeners input parameters and how to configure it see “Parameters” in [Configuration section](http://reportportal.io/docs/JVM-based-clients-configuration).
Which are common for all **JVM based** agents.

### Listener class:
`com.epam.reportportal.testng.ReportPortalTestNGListener`

There are several ways how to install listener:

- [Maven Surefire plugin](https://github.com/reportportal/agent-java-testNG#maven-surefire-plugin)
- [Specify listener in testng.xml](https://github.com/reportportal/agent-java-testNG#specify-listener-in-testngxml)
- [Custom runner](https://github.com/reportportal/agent-java-testNG#custom-runner)
- [Using command line](https://github.com/reportportal/agent-java-testNG#using-command-line)
- [Using \@Listeners annotation](https://github.com/reportportal/agent-java-testNG#using-listeners-annotation)
- [Using ServiceLoader](https://github.com/reportportal/agent-java-testNG#using-serviceloader)

> Please note, that listener must be configured in a single place only.
> Configuring multiple listeners will lead to incorrect application behavior.

### Maven Surefire plugin

Maven surefire plugin allows configuring multiple custom listeners. For logging
to Report Portal user should add Report Portal listener to “Listener” property.

```xml
<plugins>
    [...]
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.15</version>
        <configuration>
          <properties>
            <property>
              <name>usedefaultlisteners</name>
              <value>false</value> <!-- disabling default listeners is optional -->
            </property>
            <property>
              <name>listener</name>
              <value>com.epam.reportportal.testng.ReportPortalTestNGListener</value>
            </property>
          </properties>
        </configuration>
      </plugin>
    [...]
</plugins>
```

>   If you use maven surefire plugin set report portal listener only in
>   "listener" property in pom.xml.

### Specify listener in testng.xml

Here is how you can define report portal listener in your testng.xml file.

```xml
<suite>
   
  <listeners>
    <listener class-name="com.example.MyListener" />
    <listener class-name="com.epam.reportportal.testng.ReportPortalTestNGListener" />
  </listeners>
.....
```

### Custom runner

TestNG allows users to create own runners. In this case user can instantiate
listener by himself and add it to TestNG object.

```java
ReportPortalTestNGListener listener = new ReportPortalTestNGListener();
TestNG testNg = new TestNG();
List<String> lSuites = Lists.newArrayList();
Collections.addAll(lSuites, “testng_suite.xml”);
testNg.setTestSuites(lSuites);
testNg.addListener((Object) listener);
testNg.run();
```

### Using command line

Assuming that you have TestNG and Report Portal client jars in your class path,
you can run TestNG tests with Report Portal listener as follows:

*java org.testng.TestNG testng1.xml –listener
com.epam.reportportal.testng.ReportPortalTestNGListener*

### Using \@Listeners annotation

Report Portal listener can be set using \@Listener annotation.

```java
@Listeners({ReportPortalTestNGListener.class})
public class FailedParameterizedTest {
…..
}
```

\@Listeners annotation will apply to your entire suite file, just as if you had
specified it in a testng.xml file. Please do not restrict its scope.

### Using ServiceLoader

JDK offers a very elegant mechanism to specify implementations of interfaces on
the class path via the
[ServiceLoader](<http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html>)
class. With ServiceLoader, all you need to do is create a jar file that contains
your listener(s) and a few configuration files, put that jar file on the
classpath when you run TestNG and TestNG will automatically find them. You can
use service loaded mechanism for adding Report Portal listener to test
execution. Please see detailed information here:

<http://testng.org/doc/documentation-main.html#testng-listeners> 5.17.2 -
Specifying listeners with ServiceLoader.


## Code example How to overload params in run-time

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

## Example repository

There are two modules under Example project which represent agent usage with Lo4j and Logback loggers:
* https://github.com/reportportal/examples-java/tree/master/example-testng-log4j
* https://github.com/reportportal/examples-java/tree/master/example-testng-logback

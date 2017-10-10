# client-java-testng
[![Build Status](https://travis-ci.org/reportportal/agent-java-testNG.svg?branch=master)](https://travis-ci.org/reportportal/agent-java-testNG)
[ ![Download](https://api.bintray.com/packages/epam/reportportal/agent-java-testng/images/download.svg) ](https://bintray.com/epam/reportportal/agent-java-testng/_latestVersion)
 
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)

## Listeners
#### ReportPortalTestNGListener
This listener works well if you does not use testng.xml, or if you have well-structured tests in testng.xml.
In case in your testng.xml, in test tag, you have multiple classes, this listener will save them 
all in one group, use ReportPortalTestNGListenerGropByClass listener in this case.

![Methods get messed in parallel run](Images/ReportPortalTestNGListener.png)

#### ReportPortalTestNGListenerGropByClass 
This class ignores test tag and group tests by class name instead. Use it in case you have single test tag in testng.xml that refer to multiple test classes.

![Methods where grouped by classes](Images/ReportPortalTestNGListenerGropByClass.png)

##### Override UUID in run-time
```java
import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.guice.ConfigurationModule;
import com.epam.reportportal.guice.ReportPortalClientModule;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import rp.com.google.inject.Module;
import rp.com.google.inject.util.Modules;


public class MyListener extends ReportPortalTestNGListener {
    public MyListener() {
        super(Injector.create(Modules.combine(Modules.override(new ConfigurationModule())
                        .with(new Module() {
                            @Override
                            public void configure(Binder binder) {
                                Properties overrides = new Properties();
                                overrides.setProperty(ListenerProperty.UUID.getPropertyName(), "my crazy uuid");
                                PropertiesLoader propertiesLoader = PropertiesLoader.load();
                                propertiesLoader.overrideWith(overrides);
                                binder.bind(PropertiesLoader.class).toInstance(propertiesLoader);
                            }
                        }),
                new ReportPortalClientModule()
        )));
    }
}
```

# What does this test App do?

**TestMetricsLoggerAppConfig** verifies *MetricsLogger plugin* takes into account the following configurations set in the app's *Config.groovy*
* grails.plugin.metricslogger.requestHeadersForLogging
* grails.plugin.metricslogger.supportedServletSpecBelow3

# How to run the tests?

From the root directory of the test application(`...MetricsLogger/test/projects/TestMetricsLoggerAppConfig`) run these commands:

```
 grails clean
 grails compile
 grails test-app
```

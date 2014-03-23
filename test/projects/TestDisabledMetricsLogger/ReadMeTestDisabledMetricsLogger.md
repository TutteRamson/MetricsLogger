# What does this test App do?

**TestDisabledMetricsLogger** verifies when *MetricsLogger plugin* is disabled using the flag
`grails.plugin.metricslogger.disable = true` in *Config.groovy*
it doesn't intercept methods and no log entries are written to *metric.log* file.

# How to run the tests?

From the root directory of the test application(`...MetricsLogger/test/projects/TestDisabledMetricsLogger`) run these commands:

```
 grails clean
 grails compile
 grails test-app
```
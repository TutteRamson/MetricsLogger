# What does this test App do?

**TestMetricsLogger** verifies the standard behaviour of *MetricsLogger plugin* by inspecting the log entries written to *metric.log* file.
That includes *Timers* created from annotations & Start/Stop Timer calls, *Numerics* and *Fields* triggered from test app's services. It also verifies the
thread safe behaviour of the plugin by ensuring two simultaneously executed requests don't get their log entry values mixed up or overwritten.

# How to run the tests?

From the root directory of the test application(`...MetricsLogger/test/projects/TestMetricsLogger`) run these commands:

```
 grails clean
 grails compile
 grails test-app
```
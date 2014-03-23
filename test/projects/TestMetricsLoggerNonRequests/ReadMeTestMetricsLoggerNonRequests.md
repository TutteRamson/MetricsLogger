# What does this test App do?

**TestMetricsLoggerNonRequests** has a quartz job that runs once a minute and fakes some clean up job.
The clean up job creates a metric log entry on each of its run. These log entries are created differently from the usual ones
that are created as part of serving web requests.

# How to verify Metric Log entries created by non-requests?

From the root directory of the test application(`...MetricsLogger/test/projects/TestMetricsLoggerNonRequests`) run these commands:

```
 grails clean
 grails compile
 grails run-app
```
Now tail the *metric.log*: `tail -f metric.log`

You should see a new log entry added once every minute as below:

```

Time=2014-03-23T00:02:43.827Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=0,dealingWithStandAloneLogEntry=1
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

Time=2014-03-23T00:03:43.675Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=0,dealingWithStandAloneLogEntry=0
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

Time=2014-03-23T00:04:43.675Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=0,dealingWithStandAloneLogEntry=0
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

```
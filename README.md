# Metrics Logger Plugin
> This plugin borrowed ideas from [Profile Plugin](http://grails.org/plugin/profiler). Thanks to its developers.

Metrics Logger Plugin logs metrics information related to web requests served by Grails application in a thread-safe way. By configuring the logger appropriately
metrics information can be routed to a dedicated log file, which can be indexed by tools like [Splunk](http://www.splunk.com/). The plugin can also be used to log
interesting metrics outside the context of a request - for instance during application bootstrap or when batch jobs are executed. Here is a sample Metrics Log Entry:

```
Time=2014-02-17T15:31:00.840Z
Application=TestMetricsLoggerAppConfig
Timers:methodWithPlainAnnotation=30,CustomizedTimer=0,methodDefinedWith_def=0,StartStopTimer=2,ControllerDuration=89,ViewDuration=51,RequestDuration=175
Fields:CardUsed=Debit
Numerics:NumberOfItems=25.0
Request:Action=index,Controller=configValidation,Uri=/TestMetricsLoggerAppConfig/configValidation,Host=localhost,ServerName=localhost,RemoteAddress=127.0.0.1,X-Forwarded-For=null,X-Teros-Client-IP=null
Response:StatusCode=404
---EOE-----------------------------------------------------

```

# Dissection of Metrics Log Entry
* `Time` In case of requests this is when the request is intercepted. In case of manually created Log Entries this is when Log Entry is instantiated.
* `Application` Grails application name. This value is extracted from *app.name* defined in *application.properties* file.
* `Timers` List of Key-Value pairs. Keys indicate method names or custom names given to block of code that is instrumented. Values represent their duration in milliseconds.
* `Fields` List of Key-Value pairs. Keys and values are both *Strings*.
* `Numerics` List of Key-Value pairs. Keys are *Strings* and values are *Doubles*.
* `Request` Request parameters extracted from the web request.
* `Response` Response parameters extracted from the web request response.
* `---EOE---` Log Entry separator

# Integration with Metrics Logger Plugin

## Declaring dependency on Metrics Logger
* In *BuildConfig.groovy* declare a compilation dependency on *metrics-logger* in *plugins* block as below:

```groovy
    plugins {
        ...
        ...
        compile ":metrics-logger:1.1"
    }
```

* Ensure the repositories block contains `grailsPlugins()`:

```groovy
    repositories {
        grailsPlugins()
        ...
        ...
    }
```

* **Optionally** declare a compilation dependency on *apache-log4j-extras* in *dependencies* block if your application uses *log4j*:

```groovy
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes.
        ...
        compile 'log4j:apache-log4j-extras:1.2.17'
    }
```

## Configuring Appender and Logger
The instructions below are applicable if the Grails App uses log4j. If it uses another logging library, set up similar configuration using that library specific
classes and recommendation.
* Import the following classes in *Config.groovy*

```groovy
import org.apache.log4j.rolling.RollingFileAppender
import org.apache.log4j.rolling.TimeBasedRollingPolicy
```

* Create a new *Appender* for *MetricsLogger* used by the plugin and associate the logger and appender

```groovy
log4j = {
    appenders {
        ...
        appender name: 'metricLogAppender',
            new RollingFileAppender(
                         layout:pattern(conversionPattern: '%m%n'),
                         rollingPolicy:new TimeBasedRollingPolicy(
                                                   activeFileName:"metric.log",
                                                   fileNamePattern:"metric.log.%d{yyyy-MM-dd-HH}.gz"
                                                   )
                         )
    }
    ...
    info metricLogAppender: 'MetricsLogger', additivity: false
}
```

Compile and run the app. It will generate an hourly rotating log file named metric.log containing basic request metrics like request timestamp, controller/view/request duration,
host name, request and response parameters.

# Other Configurations supported by Metrics Logger Plugin
The behaviour of Metrics Logger Plugin can be controlled from the application by means of the following configuration values. These config values in *Config.groovy*
can be used to decide the behaviour at environment level or across all environments.
## Disabling the plugin

```groovy
grails.plugin.metricslogger.disable = true
```

By default the plugin is enabled to intercept requests and log metrics. Setting `grails.plugin.metricslogger.disable` flag to `true` ensures requests are not intercepted.
However if the application logs metrics for non requests by directly instantiating `LogEntry` objects as explained later in this document, they will continue to work despite
the status of this flag.

## Configuring Request Headers for extraction
The plugin extracts certain values from *HttpServletRequest*. Using the following configuration the app can specify a list of request headers that needs to be extracted
for metrics logging.

```groovy
grails.plugin.metricslogger.requestHeadersForLogging = ['X-Forwarded-For', 'X-Requested-With']
```

The above config will produce Metrics Log Entries as below:

```
Time=2014-02-17T15:31:00.840Z
Application=TestMetricsLoggerAppConfig
Timers:...
Fields:...
Numerics:...
Request:Action=index,Controller=configValidation,Uri=/TestMetricsLoggerAppConfig/configValidation,Host=localhost,ServerName=localhost,RemoteAddress=127.0.0.1,X-Forwarded-For=89.11.43.37,X-Requested-With=XMLHttpRequest
Response:...
---EOE-----------------------------------------------------

```

## Supported Servlet Spec
The plugin extracts status code of response from *HttpServletResponse* and surfaces that vital information as below:

```
Time=2014-02-17T15:31:00.840Z
Application=TestMetricsLoggerAppConfig
Timers:...
Fields:...
Numerics:...
Request:...
Response:StatusCode=404
---EOE-----------------------------------------------------

```

For that to work, the servlet container where the application is running should support servlet spec 3.0 or above. If that is not the case and you can't upgrade the
servlet container use the following flag to disable the plugin from extracting status code. Otherwise it results in method not found exception at runtime.
For efficiency reasons the plugin doesn't do try/catch in that situation.

```groovy
grails.plugin.metricslogger.supportedServletSpecBelow3 = true
```


# Adding Timers
Methods defined in grails service beans can be timed using `@LogMetrics` annotation. Blocks of code can be instrumented using `MetricsLogger.startTimer("TimerName")`
and a matching `MetricsLogger.stopTimer("TimerName")` calls. Here is an example:

```groovy
import com.mastercard.labs.metricslogger.annotation.LogMetrics
import com.mastercard.labs.metricslogger.MetricsLogger
import org.apache.log4j.Logger

class AnnotationService {
    Logger logger = Logger.getLogger(AnnotationService.class);

    @LogMetrics
    void methodWithPlainAnnotation() {
        logger.info("Method that has @LogMetrics annotation is running. There will be a timer with the name 'methodWithPlainAnnotation' in metrics log")
    }

    @LogMetrics(value="CustomizedTimer")
    void methodWithValueAnnotation() {
        logger.info("Method that has @LogMetrics annotation with a value explicitly set is running. There will be a timer named 'CustomizedTimer' in metrics log")
    }

    void methodWithoutAnnotation() {
        logger.info("Method that is not annotated is running. There should not be any timer named 'methodWithoutAnnotation' in metrics log")
    }

    @LogMetrics
    def methodDefinedWith_def() {
        logger.info("Method defined in groovy style with 'def' is running. There should be a timer named 'methodDefinedWith_def' in metrics log")
    }

    void instrumentBlockOfCode() {
        MetricsLogger.startTimer("BlockDuration")
        try {
            ...
            ...
        } finally {
            MetricsLogger.stopTimer("BlockDuration")
        }
    }
}
```

This produces a Metrics Log Entry with *timers* as shown below. *ControllerDuration*, *ViewDuration* and *RequestDuration* are calculated by the plugin automatically. The other
timer values come from the above code. The timer values are in milliseconds.

```
Time=2014-02-17T15:27:54.872Z
Application=TestMetricsLogger
Timers:methodWithPlainAnnotation=22,CustomizedTimer=1,methodDefinedWith_def=0,BlockDuration=2,ControllerDuration=88,ViewDuration=50,RequestDuration=172
Fields:...
Numerics:...
Request:...
Response:...
---EOE-----------------------------------------------------

```

**Caveat:**
> For @LogMetrics annotation to work the annotated methods should be invoked from a service bean handle. Example: `annotationService.methodWithPlainAnnotation()`

```groovy
class AnnotationService {
    Logger logger = Logger.getLogger(AnnotationService.class);

    @LogMetrics
    void annotatedMethod() {
        logger.info("Even though this method is annotated it will not be instrumented when invoked from explainCaveat() method below")
    }

    void explainCaveat() {
        annotatedMethod()
    }
}
```

# Adding Fields
Field values can be created using `MetricsLogger.addField(key, value)` method. The **value will be overwritten** if `MetricsLogger.addField()` is called with the same key again
within the scope of a request.

```groovy
import com.mastercard.labs.metricslogger.MetricsLogger

class FieldService {
    void addFieldValues() {
        MetricsLogger.addField("CardUsed", "Debit")
        MetricsLogger.addField("CountryCode", "AUS")
        ...
        MetricsLogger.addField("Merchant", "CornerShop")
        MetricsLogger.addField("CountryCode", "IRL")
    }
}
```

The above code will produce *Fields* as below if it gets executed while handling a request:

```
Time=2014-02-17T16:27:54.872Z
Application=TestMetricsLogger
Timers:...
Fields:CardUsed=Debit,CountryCode=IRL,Merchant=CornerShop
Numerics:...
Request:...
Response:...
---EOE-----------------------------------------------------

```

# Adding Numerics
Numeric values can be created in metrics log using `MetricsLogger.addNumeric(key, value)` method. The **value will be summed** if `MetricsLogger.addNumeric()` is called with the
same key again within the scope of a request.

```groovy
import com.mastercard.labs.metricslogger.MetricsLogger

class NumericService {
    void createNumericValues() {
        MetricsLogger.addNumeric("NumberOfItems", 25)
        MetricsLogger.addNumeric("SuccessCount", 1)
        ...
        MetricsLogger.addNumeric("OrderAmount", 123.45)
        MetricsLogger.addNumeric("SuccessCount", 1)
    }
}
```

The above code will produce *Numerics* as below if it gets executed while handling a request:

```
Time=2014-02-17T17:27:54.872Z
Application=TestMetricsLogger
Timers:...
Fields:...
Numerics:NumberOfItems=25.0,SuccessCount=2.0,OrderAmount=123.45
Request:...
Response:...
---EOE-----------------------------------------------------

```

# Creating Metrics Log Entry for non request
Metrics Log Entries are not restricted to web requests served by grails app. They can be created outside the context of a request and flushed out when it is populated with
enough metrics. For example batch/quartz jobs can be instrumented this way. Application bootstrap can create a metrics log entry and that can be used to correlate events
that occur following a restart or new deployment of the app.

```groovy
import com.mastercard.labs.metricslogger.LogEntry

class FakeCleanUpJob {
    static triggers = {
      simple repeatInterval: 60000l // execute job once a minute
    }

    def execute() {
        dealWithStandAloneLogEntry()
    }

    void dealWithStandAloneLogEntry() {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("dealingWithStandAloneLogEntry");
        logEntry.addField("QuartzJobName", "PeriodicCleanUp");
        logEntry.addNumeric("NumberOfItemsToCleanUp", 200);

            logEntry.startTimer("cleanUp");
            logEntry.addField("HostName", "cleanup-host");
            logEntry.addNumeric("NumberOfItemsSuccessfullyCleaned", 190);
            logEntry.addNumeric("NumberOfItemsFailedToBeCleaned", 10);
            logEntry.addNumeric("SpaceSavedFromTheCleanUpInGB", 19.4);
            logEntry.stopTimer("cleanUp");

        logEntry.stopTimer("dealingWithStandAloneLogEntry");
        logEntry.flush();
    }
}
```

The above code produces Metrics Log Entries like below:

```
Time=2014-02-17T15:34:12.417Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=1,dealingWithStandAloneLogEntry=2
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

 Time=2014-02-17T15:35:12.214Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=0,dealingWithStandAloneLogEntry=0
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

```


/*
 * Copyright (c) 2014, MasterCard
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package testmetricslogger

import groovyx.net.http.RESTClient

import org.apache.commons.lang3.StringUtils

import com.grailsrocks.functionaltest.*

class MultiThreadedFunctionalTests extends MetricsLoggerTestCase{
    void testParallelRequestsNotOverwritingMetricLogEntries() {
        def thread1 = Thread.start {
      //      get "/sleep" // when requests are made this way in multithreaded scenario, depending on the project settings/grails-groovy versions - sometimes only one of the threads succeeds.
           def client = new RESTClient( 'http://localhost:8080/TestMetricsLogger/' )
           def resp = client.get( path : 'sleep/index' )
        }

        def thread2 = Thread.start {
      //      get "/sleep"
            def client = new RESTClient( 'http://localhost:8080/TestMetricsLogger/' )
            def resp = client.get( path : 'sleep/index' )
        }

        thread1.join()
        thread2.join()

        assertMetricLogContent()
    }

    private assertMetricLogContent() {
        def logFile = new File("metric.log")
        assert logFile.exists()
        def logContent = []
        logContent = logFile.readLines()

/* Sample LogEntries from 2 threads

Time=2014-02-17T15:27:55.171Z
Application=TestMetricsLogger
Timers:PrepareSleeping=324,ActualSleeping=185,finishSleeping=204,ControllerDuration=741,ViewDuration=21,RequestDuration=764
Fields:ThreadName=http-bio-8080-exec-6
Numerics:FixedNumber=100.0
Request:Action=index,Controller=sleep,Uri=/TestMetricsLogger/sleep/index,Host=localhost,ServerName=localhost,RemoteAddress=127.0.0.1
Response:StatusCode=404
---EOE-----------------------------------------------------

Time=2014-02-17T15:27:55.172Z
Application=TestMetricsLogger
Timers:PrepareSleeping=233,ActualSleeping=406,finishSleeping=315,ControllerDuration=978,ViewDuration=0,RequestDuration=981
Fields:ThreadName=http-bio-8080-exec-7
Numerics:FixedNumber=100.0
Request:Action=index,Controller=sleep,Uri=/TestMetricsLogger/sleep/index,Host=localhost,ServerName=localhost,RemoteAddress=127.0.0.1
Response:StatusCode=404
---EOE-----------------------------------------------------

*/

        // assert there are 2 log entries
        assert logContent.findAll{ it =~ /Time=\d{4}-\d{2}-\d{2}T/}.size() == 2

        // assert fields set by different threads are not overwritten
        def fieldsList = []
        fieldsList = logContent.findAll() { it =~ /Fields:/ }
        String threadName1 =  StringUtils.substringAfter(fieldsList[0], "=")
        String threadName2 =  StringUtils.substringAfter(fieldsList[1], "=")
        assert ! threadName1.equals(threadName2)

        // assert that Numeric value set by threads are not accumulated
        def numericList = []
        fieldsList = logContent.findAll() { it =~ /Numerics:/ }
        numericList.each { assert it ==~ /Numerics:FixedNumber=100.0/ }

        // assert that Timers are not repeated
        def timerList = []
        timerList = logContent.findAll() { it =~ /Timers:/ }
        timerList.each { assert StringUtils.countMatches(it, "PrepareSleeping") == 1
            assert StringUtils.countMatches(it, "ActualSleeping") == 1
            assert StringUtils.countMatches(it, "finishSleeping") == 1
        }
    }
}

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

import org.apache.commons.lang3.StringUtils

import com.grailsrocks.functionaltest.*

class AnnotationFunctionalTests extends MetricsLoggerTestCase {
    void testAnnotatedServieMethodsFindingTheirWayToMetricLog() {
        get "/annotation"
        assertExpectedMetricLogLinesArePresent()
        assertMetricLogContent()
    }

    private assertMetricLogContent() {
        def logFile = new File("metric.log")
        assert logFile.exists()
        def logContent = []
        logContent = logFile.readLines()

/* Sample LogEntry that gets written in metric.log

Time=2014-02-17T15:27:54.872Z
Application=TestMetricsLogger
Timers:methodWithPlainAnnotation=22,CustomizedTimer=1,methodDefinedWith_def=0,StartStopTimer=2,ControllerDuration=88,ViewDuration=50,RequestDuration=172
Fields:TestField=TestValue
Numerics:NumberOfItems=25.0
Request:Action=index,Controller=annotation,Uri=/TestMetricsLogger/annotation,Host=localhost,ServerName=localhost,RemoteAddress=127.0.0.1
Response:StatusCode=404
---EOE-----------------------------------------------------

*/
        def timerLine = logContent.find() { it =~ /Timers:/ }
        assert StringUtils.countMatches(timerLine, "methodWithPlainAnnotation") == 1
        assert StringUtils.countMatches(timerLine, "CustomizedTimer") == 1
        assert StringUtils.countMatches(timerLine, "methodDefinedWith_def") == 1
        assert StringUtils.countMatches(timerLine, "StartStopTimer") == 1
        assert StringUtils.countMatches(timerLine, "methodWithoutAnnotation") == 0
    }

    private assertExpectedMetricLogLinesArePresent() {
        def logFile = new File("metric.log")
        assert logFile.exists()
        def logContent = []
        logContent = logFile.readLines()

        assert logContent.size() == 9 //including the blank line at the end of a Metric Log Entry

        def logLine = logContent[0]
        assert logLine =~ /Time=\d{4}-\d{2}-\d{2}T/

        logLine = logContent[1]
        assert logLine ==~ /Application=TestMetricsLogger/

        logLine = logContent[2]
        assert logLine =~ /Timers:/

        logLine = logContent[3]
        assert logLine =~ /Fields:TestField=TestValue/

        logLine = logContent[4]
        assert logLine =~ /Numerics:NumberOfItems=25.0/

        logLine = logContent[5]
        assert logLine =~ /Request:/

        logLine = logContent[6]
        assert logLine =~ /Response:/

        logLine = logContent[7]
        assert logLine ==~ /---EOE-----------------------------------------------------/
    }
}
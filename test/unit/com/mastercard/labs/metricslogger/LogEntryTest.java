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

package com.mastercard.labs.metricslogger;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.web.context.request.RequestContextHolder;

import com.mastercard.labs.metricslogger.config.AppConfigReader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RequestContextHolder.class, AppConfigReader.class})
public class LogEntryTest {

    @BeforeClass
    public static void setUpOnce() {
        PowerMockito.mockStatic(AppConfigReader.class);
        when(AppConfigReader.getRequestHeadersForLogging()).thenReturn(Arrays.asList("X-Forwarded-For","X-Teros-Client-IP"));
        when(AppConfigReader.getSupportedServletSpecBelow3()).thenReturn(false);
    }

    @Test
    public void testExpectedLinesInLogEntry() {
        String entry = new LogEntry().toString();
        String[] entryLines = entry.split("\n");
        assertThat(entryLines.length, is(8));
        assertThat(entryLines[0], startsWith("Time="));
        assertThat(entryLines[1], startsWith("Application="));
        assertThat(entryLines[2], startsWith("Timers:"));
        assertThat(entryLines[3], startsWith("Fields:"));
        assertThat(entryLines[4], startsWith("Numerics:"));
        assertThat(entryLines[5], startsWith("Request:null"));
        assertThat(entryLines[6], startsWith("Response:null"));
        assertThat(entryLines[7], equalTo("---EOE-----------------------------------------------------"));
    }

    @Test
    public void testTimersInSequence() throws InterruptedException {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("TimerWith2DigitSleep");
        Thread.sleep(10);
        logEntry.stopTimer("TimerWith2DigitSleep");

        logEntry.startTimer("TimerWith3DigitSleep");
        Thread.sleep(100);
        logEntry.stopTimer("TimerWith3DigitSleep");

        String[] entryLines = logEntry.toString().split("\n");
        assertThat(entryLines[2], allOf(containsString("Timers:"), containsString("TimerWith2DigitSleep="), containsString("TimerWith3DigitSleep=")));
        assertTrue("Timer values didn't match expectation", entryLines[2].matches("^Timers:TimerWith2DigitSleep=\\d{2},TimerWith3DigitSleep=\\d{3}$"));
    }

    @Test
    public void testNestedTimers() throws InterruptedException {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("OuterTimer");
        Thread.sleep(10);
        logEntry.startTimer("InnerTimer");
        Thread.sleep(20);
        logEntry.stopTimer("InnerTimer");
        logEntry.stopTimer("OuterTimer");
        String[] entryLines = logEntry.toString().split("\n");
        Pattern p = Pattern.compile("^Timers:InnerTimer=(\\d+),OuterTimer=(\\d+)$");
        Matcher m = p.matcher(entryLines[2]);
        assertTrue("Timer values don't match expectation", m.matches());
        assertTrue("Timers don't have the right relative values", new Integer(m.group(1)) < new Integer(m.group(2)));
    }

    @Test
    public void testInterleavedTimers() throws InterruptedException {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("FristTimer");
        Thread.sleep(10);
        logEntry.startTimer("SecondTimer");
        Thread.sleep(10);
        logEntry.stopTimer("FirstTimer");
        Thread.sleep(10);
        logEntry.stopTimer("SecondTimer");
        String[] entryLines = logEntry.toString().split("\n");
        assertTrue("Interleaved timers are not in the correct order", entryLines[2].matches("^Timers:FirstTimer=(\\d+),SecondTimer=(\\d+)$"));
    }

    @Test
    public void testRepeatedTimers() throws InterruptedException {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("RepeatTimer");
        Thread.sleep(10);
        logEntry.stopTimer("RepeatTimer");

        logEntry.startTimer("SingleOccurrenceTimer");
        Thread.sleep(15);
        logEntry.stopTimer("SingleOccurrenceTimer");

        logEntry.startTimer("RepeatTimer");
        Thread.sleep(2);
        logEntry.stopTimer("RepeatTimer");

        String[] entryLines = logEntry.toString().split("\n");
        assertThat(StringUtils.countMatches(entryLines[2], "RepeatTimer="), is(2));
        assertTrue("Repeated timers pattern doesn't meet expecations", entryLines[2].matches("^Timers:RepeatTimer=(\\d+),SingleOccurrenceTimer=(\\d+),RepeatTimer=(\\d+)$"));
    }

    @Test
    public void testAddField() {
        LogEntry logEntry = new LogEntry();
        logEntry.addField("Field1", "Value1");
        logEntry.addField("FieldToBeOverWritten", "ValueBeforeOverWritten");
        logEntry.addField("Field3", "Value3");
        logEntry.addField("FieldToBeOverWritten", "ValueAfterOverWritten");

        String[] entryLines = logEntry.toString().split("\n");
        assertThat(StringUtils.countMatches(entryLines[3], "Field1"), is(1));
        assertThat(StringUtils.countMatches(entryLines[3], "FieldToBeOverWritten"), is(1));
        assertThat(StringUtils.countMatches(entryLines[3], "Field3"), is(1));

        assertThat(entryLines[3], containsString("Field1=Value1"));
        assertThat(entryLines[3], containsString("Field3=Value3"));
        assertThat(entryLines[3], containsString("FieldToBeOverWritten=ValueAfterOverWritten"));

        assertThat(entryLines[3], not(containsString("ValueBeforeOverWritten")));
    }

    @Test
    public void testAddNumeric() {
        LogEntry logEntry = new LogEntry();

        logEntry.addNumeric("Counter1", 10.0);
        logEntry.addNumeric("RepeatedCounter", 25.0);
        logEntry.addNumeric("Counter3", 5.0);
        logEntry.addNumeric("RepeatedCounter", 75.0);

        String[] entryLines = logEntry.toString().split("\n");
        assertThat(StringUtils.countMatches(entryLines[4], "Counter1=10.0"), is(1));
        assertThat(StringUtils.countMatches(entryLines[4], "Counter3=5.0"), is(1));

        assertThat(StringUtils.countMatches(entryLines[4], "RepeatedCounter"), is(1));
        assertThat(entryLines[4], containsString("RepeatedCounter=100.0"));
    }

    @Test
    public void testRequestAndResponseParamExtraction() {
        boolean withRequestHeaders = true;
        prepareForRequestResponseExtraction(withRequestHeaders);

        String[] entryLines = new LogEntry().toString().split("\n");
        assertThat(entryLines[5], is("Request:Action=TestAction,Controller=test,Uri=/test/TestAction,Host=apphost1.company.com,ServerName=app.company.com,RemoteAddress=99.501.123.10,X-Forwarded-For=99.501.123.10,X-Teros-Client-IP=null"));
        assertThat(entryLines[6], is("Response:StatusCode=200"));
    }

    private void prepareForRequestResponseExtraction(boolean withRequestHeaders) {
        PowerMockito.mockStatic(RequestContextHolder.class);
        GrailsWebRequest mockWebRequest = mock(GrailsWebRequest.class);
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockHttpResponse = mock(HttpServletResponse.class);

        when(RequestContextHolder.getRequestAttributes()).thenReturn(mockWebRequest);
        when(mockWebRequest.getActionName()).thenReturn("TestAction");
        when(mockWebRequest.getControllerName()).thenReturn("test");
        when(mockWebRequest.getCurrentRequest()).thenReturn(mockHttpRequest);

        when(mockHttpRequest.getRequestURI()).thenReturn("/test/TestAction");
        when(mockHttpRequest.getLocalName()).thenReturn("apphost1.company.com");
        when(mockHttpRequest.getServerName()).thenReturn("app.company.com");
        when(mockHttpRequest.getRemoteAddr()).thenReturn("99.501.123.10");
        if (withRequestHeaders == true)
            prepareForRequestHeaderExtraction(mockHttpRequest);

        when(mockWebRequest.getCurrentResponse()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatus()).thenReturn(200);
    }

    private void prepareForRequestHeaderExtraction(HttpServletRequest mockHttpRequest) {
        when(mockHttpRequest.getHeader("X-Forwarded-For")).thenReturn("99.501.123.10");
        when(mockHttpRequest.getHeader("X-Teros-Client-IP")).thenReturn(null);
    }

    @Test
    public void testEmptyRequestHeaders() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean withoutRequestHeaders = false;
        prepareForRequestResponseExtraction(withoutRequestHeaders);

        Field f = LogEntry.class.getDeclaredField("requestHeadersForLogging");
        f.setAccessible(true);
        f.set(null, new ArrayList<String>());

        String[] entryLines = new LogEntry().toString().split("\n");
        assertThat(entryLines[5], is("Request:Action=TestAction,Controller=test,Uri=/test/TestAction,Host=apphost1.company.com,ServerName=app.company.com,RemoteAddress=99.501.123.10"));
        assertThat(entryLines[6], is("Response:StatusCode=200"));
    }

    @Test
    public void testSupportedServletSpecBelow3() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        boolean withoutRequestHeaders = false;
        prepareForRequestResponseExtraction(withoutRequestHeaders);

        Field f = LogEntry.class.getDeclaredField("supportedServletSpecBelow3");
        f.setAccessible(true);
        f.set(null, true);

        String[] entryLines = new LogEntry().toString().split("\n");
        assertThat(entryLines[6], is("Response:null"));

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.getRequestAttributes();
        verify(webRequest, never()).getCurrentResponse();
    }

}

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

/**
 * LogEntry that holds timing, numeric, field values and request attributes of a single request.
 */
package com.mastercard.labs.metricslogger;

import grails.util.Metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.mastercard.labs.metricslogger.config.AppConfigReader;

public class LogEntry {
    public static final String LOGGER_NAME = "MetricsLogger";
    private DateTime requestStartTime = new DateTime();
    private String applicationName = Metadata.getCurrent().getApplicationName();
    private static final String ENTRY_SEPARATOR = "\n---EOE-----------------------------------------------------\n";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final int INITIAL_CAPACITY_1K = 1024;
    private static final int INITIAL_CAPACITY_256B = 256;
    private static final char LIST_SEPARATOR = ',';
    private static List<String> requestHeadersForLogging = AppConfigReader.getRequestHeadersForLogging();
    private static boolean supportedServletSpecBelow3 = AppConfigReader.getSupportedServletSpecBelow3();

    private Stack<Long> startTimes = new Stack<Long>();
    private ArrayList<String> timers = new ArrayList<String>();
    private HashMap<String, String> fields = new HashMap<String, String>();
    private HashMap<String, Double> numerics = new HashMap<String, Double>();

    public void startTimer(String timerName) {
        // with the stack based implementation of timers, the timerName is not required.
        // But later if we change the underlying data structure, timerName might come handy and callers possibly don't have to change.
        // Also at the call location the timerName serves as documentation, depending on how one sees it.
        startTimes.push(System.currentTimeMillis());
    }

    public void stopTimer(String timerName) {
        if (startTimes.empty()) {
            return;
        }
        long endTime = System.currentTimeMillis();
        Long startTime = startTimes.pop();
        long timerValue = endTime - startTime;
        StringBuilder timerNameValue = new StringBuilder(timerName).append(KEY_VALUE_SEPARATOR).append(timerValue);
        timers.add(timerNameValue.toString());
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(INITIAL_CAPACITY_1K);
        output.append("Time=").append(requestStartTime);
        output.append("\nApplication=").append(applicationName);
        output.append("\nTimers:").append(StringUtils.join(timers.toArray(), LIST_SEPARATOR));
        output.append("\nFields:").append(toString(fields));
        output.append("\nNumerics:").append(toString(numerics));
        output.append("\nRequest:").append(getRequestParameters());
        output.append("\nResponse:").append(getResponseParameters());
        return output.append(ENTRY_SEPARATOR).toString();
    }

    private String getRequestParameters() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null || !(requestAttributes instanceof GrailsWebRequest)) {
            return null;
        }
        GrailsWebRequest grailsRequest = (GrailsWebRequest)requestAttributes;
        StringBuilder reqParams = new StringBuilder(INITIAL_CAPACITY_256B);
        reqParams.append("Action=").append(grailsRequest.getActionName());
        reqParams.append(",Controller=").append(grailsRequest.getControllerName());

        HttpServletRequest httpServletReq = grailsRequest.getCurrentRequest();
        reqParams.append(",Uri=").append(httpServletReq.getRequestURI());
        reqParams.append(",Host=").append(httpServletReq.getLocalName());
        reqParams.append(",ServerName=").append(httpServletReq.getServerName());
        reqParams.append(",RemoteAddress=").append(httpServletReq.getRemoteAddr());

        for (String header : requestHeadersForLogging) {
            reqParams.append(LIST_SEPARATOR).append(header).append(KEY_VALUE_SEPARATOR).append(httpServletReq.getHeader(header));
        }

        return reqParams.toString();
    }

    private String getResponseParameters() {

        if (supportedServletSpecBelow3) {
            return null;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null || !(requestAttributes instanceof GrailsWebRequest)) {
            return null;
        }
        GrailsWebRequest grailsRequest = (GrailsWebRequest)requestAttributes;
        StringBuilder responseParams = new StringBuilder(INITIAL_CAPACITY_256B);
        responseParams.append("StatusCode=").append(grailsRequest.getCurrentResponse().getStatus());

        return responseParams.toString();
    }

    public void addField(String name, String value) {
        fields.put(name, value);
    }

    public void addNumeric(String name, Double value) {
        if (value == null) {
            return;
        }
        Double previousValue = numerics.get(name);
        Double effectiveValue = previousValue == null ? value : value + previousValue;
        numerics.put(name, effectiveValue);
    }

    private <T> String toString(Map<String, T> map) {
        StringBuilder keyValues = new StringBuilder(INITIAL_CAPACITY_256B);
        for(Map.Entry<String, T> entry : map.entrySet()) {
            keyValues.append(LIST_SEPARATOR).append(entry.getKey()).append(KEY_VALUE_SEPARATOR).append(entry.getValue());
        }
        if (keyValues.length() > 0) {
            keyValues.deleteCharAt(0);
        }
        return keyValues.toString();
    }

    public void flush() {
        Logger log = LoggerFactory.getLogger(LOGGER_NAME);
        log.info(toString());
    }
}

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

package com.mastercard.labs.metricslogger.customs;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;

import com.mastercard.labs.metricslogger.MetricsLogger;
import com.mastercard.labs.metricslogger.annotation.LogMetrics;

/**
 * Interceptor configured by MetricsLogger plug-in to intercept methods in Service Beans.
 * Methods with {@link com.mastercard.labs.metricslogger.annotation.LogMetrics LogMetrics} annotation will add their timing info to Metrics Log.
 * Based on:
 * https://github.com/pledbrook/grails-profiler/blob/master/src/java/com/linkedin/grails/profiler/ProfilerMethodInterceptor.java
 */
public class MetricsLoggerMethodInterceptor implements MethodInterceptor {
    private static String BYPASS_METHOD = "getMetaClass";

    @Override
    public Object invoke(MethodInvocation paramMethodInvocation) throws Throwable {
        // Not interested in calls to getMetaClass().
        String methodName = paramMethodInvocation.getMethod().getName();

        if (BYPASS_METHOD.equals(methodName)) {
            return paramMethodInvocation.proceed();
        }

        Method interceptedMethod = paramMethodInvocation.getMethod();
        LogMetrics logMetricsAnnotation = interceptedMethod.getAnnotation(LogMetrics.class);

        if (logMetricsAnnotation == null) {
            return paramMethodInvocation.proceed();
        }

        if (StringUtils.isNotBlank(logMetricsAnnotation.value())) {
            methodName = logMetricsAnnotation.value();
        }

        MetricsLogger.startTimer(methodName);
        try {
            return paramMethodInvocation.proceed();
        } finally {
            MetricsLogger.stopTimer(methodName);
        }
    }
}

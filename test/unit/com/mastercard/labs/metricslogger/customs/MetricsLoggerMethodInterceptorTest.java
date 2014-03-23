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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mastercard.labs.metricslogger.MetricsLogger;
import com.mastercard.labs.metricslogger.annotation.LogMetrics;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetricsLogger.class)
public class MetricsLoggerMethodInterceptorTest {
    private MetricsLoggerMethodInterceptor componentUnderTest = new MetricsLoggerMethodInterceptor();
    @Mock private Method mockMethod;
    @Mock private MethodInvocation mockInvocation;

    @Before
    public void setUp() {
        when(mockInvocation.getMethod()).thenReturn(mockMethod);
        PowerMockito.mockStatic(MetricsLogger.class);
    }

    @Test
    public void testInvokeWhenMethodNameIs_getMetaClass() throws Throwable {
        when(mockMethod.getName()).thenReturn("getMetaClass");

        componentUnderTest.invoke(mockInvocation);

        verify(mockInvocation).proceed();
        verifyTimersAreUntouched();
    }

    private void verifyTimersAreUntouched() {
        PowerMockito.verifyStatic(Mockito.never());
        MetricsLogger.startTimer(Matchers.anyString());

        PowerMockito.verifyStatic(Mockito.never());
        MetricsLogger.stopTimer(Matchers.anyString());
    }

    @Test
    public void testInvokeWhenMethodIsNotAnnotated() throws Throwable {
        when(mockMethod.getAnnotation(LogMetrics.class)).thenReturn(null);

        componentUnderTest.invoke(mockInvocation);

        verify(mockInvocation).proceed();
        verifyTimersAreUntouched();
    }

    @Test
    public void testInvokeWhenMethodAnnotationIsBlank() throws Throwable {
        when(mockInvocation.getMethod()).thenReturn(this.getClass().getMethod("emptyAnnotationMethod", null));
        componentUnderTest.invoke(mockInvocation);

        verify(mockInvocation).proceed();
        verifyTimersFor("emptyAnnotationMethod");
    }

    private void verifyTimersFor(String expectedTimerName) {
        PowerMockito.verifyStatic();
        MetricsLogger.startTimer(expectedTimerName);

        PowerMockito.verifyStatic();
        MetricsLogger.stopTimer(expectedTimerName);
    }

    @Test
    public void testInvokeWhenMethodAnnotationIsNotBlank() throws Throwable {
        when(mockInvocation.getMethod()).thenReturn(this.getClass().getMethod("valueSpecifiedAnnotationMethod", null));
        componentUnderTest.invoke(mockInvocation);

        verify(mockInvocation).proceed();
        verifyTimersFor("AnnotatedValue");
    }

    @LogMetrics
    public void emptyAnnotationMethod() {
        // Method used by testInvokeWhenMethodAnnotationIsBlank()
    }

    @LogMetrics(value="AnnotatedValue")
    public void valueSpecifiedAnnotationMethod() {
        // Method used by testInvokeWhenMethodAnnotationIsNotBlank()
    }
}

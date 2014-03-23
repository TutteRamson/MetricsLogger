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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mastercard.labs.metricslogger.MetricsLogger;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetricsLogger.class)
public class MetricsLoggerFilterTest {
    private MetricsLoggerFilter componentUnderTest = new MetricsLoggerFilter();

    @Mock private HttpServletRequest mockRequest;
    @Mock private HttpServletResponse mockResponse;
    @Mock private FilterChain mockChain;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(MetricsLogger.class);
    }

    @Test
    public void testDoFilterInternalOnSuccessfulCallForwardingToFilterChain() throws ServletException, IOException {
        componentUnderTest.doFilterInternal(mockRequest, mockResponse, mockChain);

        verifyStaticCallsOnMetricsLogger();
        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    private void verifyStaticCallsOnMetricsLogger() {
        PowerMockito.verifyStatic();
        MetricsLogger.make();

        PowerMockito.verifyStatic();
        MetricsLogger.startTimer("RequestDuration");

        PowerMockito.verifyStatic();
        MetricsLogger.stopTimer("RequestDuration");

        PowerMockito.verifyStatic();
        MetricsLogger.flush();
    }

    @Test(expected = ServletException.class)
    public void testDoFilterInternalOnFailedCallForwardingToFilterChain() throws ServletException, IOException {
        doThrow(ServletException.class).when(mockChain).doFilter(Matchers.any(HttpServletRequest.class), Matchers.any(HttpServletResponse.class));

        componentUnderTest.doFilterInternal(mockRequest, mockResponse, mockChain);

        verifyStaticCallsOnMetricsLogger();
        verify(mockChain).doFilter(mockRequest, mockResponse);
    }
}

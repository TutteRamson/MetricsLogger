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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;

public class MetricsLoggerTest {
    @SuppressWarnings("unchecked")
    private static ThreadLocal<LogEntry> mockLocalLogEntry = mock(ThreadLocal.class);

    private LogEntry mockLogEntry = mock(LogEntry.class);

    @BeforeClass
    public static void setUpOnce() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = MetricsLogger.class.getDeclaredField("localLogEntry");
        f.setAccessible(true);
        f.set(null, mockLocalLogEntry);
    }

    @Test
    public void testMakeForTheFirstTime() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.make();
        verify(mockLocalLogEntry).set(Matchers.any(LogEntry.class));
    }

    @Test
    public void testMakeAfterTheFirstTime() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.make();
        verify(mockLocalLogEntry, never()).set(Matchers.any(LogEntry.class));
    }

    @Test
    public void testFlushBeforeMaking() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.flush();
        verify(mockLogEntry, never()).flush();
        verify(mockLocalLogEntry, never()).set(null);
    }

    @Test
    public void testFlushAfterMaking() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.flush();
        verify(mockLogEntry).flush();
        verify(mockLocalLogEntry).remove();
    }

    @Test
    public void testAddFieldBeforeMaking() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.addField("Name", "value");
        verify(mockLogEntry, never()).addField(Matchers.anyString(), Matchers.anyString());
    }

    @Test
    public void testAddFieldAfterMaking() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.addField("Name", "value");
        verify(mockLogEntry).addField("Name", "value");
    }

    @Test
    public void testStartTimerBeforeMaking() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.startTimer("TimerName");
        verify(mockLogEntry, never()).startTimer(Matchers.anyString());
    }

    @Test
    public void testStartTimerAfterMaking() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.startTimer("TimerName");
        verify(mockLogEntry).startTimer("TimerName");
    }

    @Test
    public void testStopTimerBeforeMaking() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.stopTimer("TimerName");
        verify(mockLogEntry, never()).stopTimer(Matchers.anyString());
    }

    @Test
    public void testStopTimerAfterMaking() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.stopTimer("TimerName");
        verify(mockLogEntry).stopTimer("TimerName");
    }

    @Test
    public void testAddNumericBeforeMaking() {
        when(mockLocalLogEntry.get()).thenReturn(null);
        MetricsLogger.addNumeric("NumericName", 20.0);
        verify(mockLogEntry, never()).addNumeric(Matchers.anyString(), Matchers.anyDouble());
    }

    @Test
    public void testAddNumericAfterMaking() {
        when(mockLocalLogEntry.get()).thenReturn(mockLogEntry);
        MetricsLogger.addNumeric("NumericName", 20.0);
        verify(mockLogEntry).addNumeric("NumericName", 20.0);
    }

}

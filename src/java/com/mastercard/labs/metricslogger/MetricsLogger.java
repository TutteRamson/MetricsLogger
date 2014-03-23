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

public class MetricsLogger {
    private static ThreadLocal<LogEntry> localLogEntry = new ThreadLocal<LogEntry>();

    /**
     * A call to {@link #make() make} should be matched by {@link #flush() flush}. Otherwise, if the current thread is part of a pool,
     * on its next work assignment values held in {@link LogEntry} from its previous work will leak though.
     * Applications typically don't have to call {@link #make() make} and {@link #flush() flush} because
     * they will be called by {@link com.mastercard.labs.metricslogger.customs.MetricsLoggerFilter}
     */
    public static void make() {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            localLogEntry.set(new LogEntry());
        }
    }

    /**
     * Applications typically don't have to call {@link #flush() flush} because
     * it will be called by {@link com.mastercard.labs.metricslogger.customs.MetricsLoggerFilter}
     * @see #make() make
     */
    public static void flush() {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            return;
        }
        logEntry.flush();
        localLogEntry.set(null);
    }

    public static void addField(String fieldName, String fieldValue) {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            return;
        }
        logEntry.addField(fieldName, fieldValue);
    }

    public static void startTimer(String timerName) {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            return;
        }
        logEntry.startTimer(timerName);
    }

    public static void stopTimer(String timerName) {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            return;
        }
        logEntry.stopTimer(timerName);
    }

    public static void addNumeric(String numericName, Double numericValue) {
        LogEntry logEntry = localLogEntry.get();
        if (logEntry == null) {
            return;
        }
        logEntry.addNumeric(numericName, numericValue);
    }
}

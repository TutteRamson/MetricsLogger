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

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import com.mastercard.labs.metricslogger.MetricsLogger;

/**
 * Handler Interceptor that adds timing information on Controller and View calls.
 * Based on:
 * https://github.com/pledbrook/grails-profiler/blob/master/src/java/com/linkedin/grails/profiler/ProfilerHandlerInterceptor.java
 */
public class MetricsLoggerHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final String ONGOING_TIMER = "com.mastercard.labs.metricslogger.OngoingTimer";
    private static final String CONTROLLER_TIMER = "ControllerDuration";
    private static final String VIEW_TIMER = "ViewDuration";

    /**
     * Called before the controller action is invoked. Starts the timer for Controller call.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        MetricsLogger.startTimer(CONTROLLER_TIMER);
        request.setAttribute(ONGOING_TIMER, CONTROLLER_TIMER);
        return true;
    }

    /**
     * Called after the controller action has been invoked, but before the view has been rendered.
     * Timer for controller is stopped and a new timer for view will be started.
     * Note that this method may not actually be called in the case of a redirect or similar.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) {
        MetricsLogger.stopTimer(CONTROLLER_TIMER);
        MetricsLogger.startTimer(VIEW_TIMER);
        request.setAttribute(ONGOING_TIMER, VIEW_TIMER);
    }

    /**
     * Called after the request has finished. It stops the ongoing timer.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) {
        String timerName = (String) request.getAttribute(ONGOING_TIMER);
        if (timerName != null) {
            MetricsLogger.stopTimer(timerName);
        }
    }
}

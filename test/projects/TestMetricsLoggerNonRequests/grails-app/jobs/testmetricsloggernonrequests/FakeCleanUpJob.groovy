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

package testmetricsloggernonrequests

import com.mastercard.labs.metricslogger.LogEntry

class FakeCleanUpJob {
    static triggers = {
      simple repeatInterval: 60000l // execute job once a minute
    }

    def execute() {
        dealWithStandAloneLogEntry()
    }
/*
This job produces entries like below in metric.log

Time=2014-02-17T15:34:12.417Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=1,dealingWithStandAloneLogEntry=2
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

 Time=2014-02-17T15:35:12.214Z
Application=TestMetricsLoggerNonRequests
Timers:cleanUp=0,dealingWithStandAloneLogEntry=0
Fields:QuartzJobName=PeriodicCleanUp,HostName=cleanup-host
Numerics:NumberOfItemsToCleanUp=200.0,SpaceSavedFromTheCleanUpInGB=19.4,NumberOfItemsSuccessfullyCleaned=190.0,NumberOfItemsFailedToBeCleaned=10.0
Request:null
Response:null
---EOE-----------------------------------------------------

 */

    void dealWithStandAloneLogEntry() {
        LogEntry logEntry = new LogEntry();
        logEntry.startTimer("dealingWithStandAloneLogEntry");
        logEntry.addField("QuartzJobName", "PeriodicCleanUp");
        logEntry.addNumeric("NumberOfItemsToCleanUp", 200);

            logEntry.startTimer("cleanUp");
            logEntry.addField("HostName", "cleanup-host");
            logEntry.addNumeric("NumberOfItemsSuccessfullyCleaned", 190);
            logEntry.addNumeric("NumberOfItemsFailedToBeCleaned", 10);
            logEntry.addNumeric("SpaceSavedFromTheCleanUpInGB", 19.4);
            logEntry.stopTimer("cleanUp");

        logEntry.stopTimer("dealingWithStandAloneLogEntry");
        logEntry.flush();
    }
}

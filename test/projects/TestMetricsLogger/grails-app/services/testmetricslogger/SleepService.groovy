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

import com.mastercard.labs.metricslogger.annotation.LogMetrics
import org.apache.log4j.Logger
import com.mastercard.labs.metricslogger.MetricsLogger
import java.util.Random

class SleepService {
    Logger logger = Logger.getLogger(SleepService.class);
    private static Random rand = new Random()

    void prepareSleeping() {
        MetricsLogger.startTimer("PrepareSleeping") // in multithreaded case, there should be only one instance of PrepareSleeping timer value per LogEntry
        logger.info("Preparations for Sleeping:" + Thread.currentThread().getName())
        MetricsLogger.addField("ThreadName", "${Thread.currentThread().getName()}") // in multithreaded case ThreadNames should be distinct and no overwriting of ThreadNames should happen
        sleep (getRandomNumberBetween(100, 500))
        MetricsLogger.stopTimer("PrepareSleeping")
    }

    @LogMetrics(value="ActualSleeping")
    void performSleeping() {
        logger.info("Actual sleeping begins:" + Thread.currentThread().getName())
        MetricsLogger.addNumeric("FixedNumber", 100)  // in multithreaded case FixedNumber's value shouldn't change
        sleep (getRandomNumberBetween(100, 500))
    }

    @LogMetrics
    void finishSleeping() {
        sleep (getRandomNumberBetween(100, 500))
        logger.info("Finished Sleeping:" + Thread.currentThread().getName())
    }

    private int getRandomNumberBetween(int min, int max) {
        return (min + rand.nextFloat() * (max - min))
    }
}
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

import grails.plugin.webxml.FilterManager

import com.mastercard.labs.metricslogger.customs.MetricsLoggerFilter
import com.mastercard.labs.metricslogger.customs.MetricsLoggerMethodInterceptor
import com.mastercard.labs.metricslogger.customs.MetricsLoggerHandlerInterceptor

import org.codehaus.groovy.grails.commons.spring.BeanConfiguration
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean

/**
 * Based on:
 * https://github.com/pledbrook/grails-profiler/blob/master/ProfilerGrailsPlugin.groovy
 */
class MetricsLoggerGrailsPlugin {
    def version = "1.1"
    def grailsVersion = "2.1 > *"
    def loadAfter = ["services", "controllers"]
    def title = "Metrics Logger Plugin"
    def author = "Ramson Tutte"
    def authorEmail = "ramson_tutte@mastercard.com"
    def description = 'Sends application and business metrics to a configured logger'
    def documentation = "https://github.com/TutteRamson/MetricsLogger/blob/master/README.md"
    def license = "BSD3"  // http://opensource.org/licenses/BSD-3-Clause

    def organization = [ name: "MasterCard", url: "http://www.mastercard.com/" ]

    def issueManagement = [ system: "GitHub", url: "https://github.com/TutteRamson/MetricsLogger/issues" ]
    def scm = [ url: "https://github.com/TutteRamson/MetricsLogger" ]

    def getWebXmlFilterOrder() { // see http://grails.org/plugin/webxml
        [metricsLoggerFilter: FilterManager.URL_MAPPING_POSITION - 75]
    }

    def doWithWebDescriptor = { xml ->

        def disableMetricsLogger = application.config.grails.plugin.metricslogger.disable
        if (disableMetricsLogger) {
            return
        }

        // Add the MetricsLoggerFilter to the web app.
        def filterDef = xml.'filter'
        filterDef[filterDef.size() - 1] + {
            filter {
                'filter-name'('metricsLoggerFilter')
                'filter-class'(MetricsLoggerFilter.name)
            }
        }

        // This filter *must* come before the urlMapping filter, otherwise it will never be executed.
        def filterMapping = xml.'filter-mapping'.find { it.'filter-name'.text() == "charEncodingFilter" }
        filterMapping + {
            'filter-mapping' {
                'filter-name'("metricsLoggerFilter")
                'url-pattern'("/*")
            }
        }
    }

    def doWithSpring = {

        def disableMetricsLogger = application.config.grails.plugin.metricslogger.disable
        if (disableMetricsLogger) {
            return
        }

        // Interceptor for timing service method invocations.
        metricsLoggerMethodInterceptor(MetricsLoggerMethodInterceptor)

        // Spring HandlerInterceptor for timing controllers and views.
        metricsLoggerHandlerInterceptor(MetricsLoggerHandlerInterceptor)

        [annotationHandlerMapping, controllerHandlerMappings]*.interceptors << metricsLoggerHandlerInterceptor

        // The existing service bean definitions are replaced with proxy beans
        if (!manager?.hasGrailsPlugin("services")) {
            return
        }

        for (serviceClass in application.serviceClasses) {
            String serviceName = serviceClass.propertyName
            BeanConfiguration beanConfig = springConfig.getBeanConfig(serviceName)
            if (!beanConfig) {
                continue
            }

            // If we're dealing with a TransactionProxyFactoryBean, then we can add the MetricsLoggerMethodInterceptor directly to it.
            if (beanConfig.beanDefinition.beanClassName == TransactionProxyFactoryBean.name) {
                if (!beanConfig.hasProperty("preInterceptors")) {
                    beanConfig.addProperty("preInterceptors", [])
                }

                delegate."$serviceName".preInterceptors << ref("metricsLoggerMethodInterceptor")
            }
            // Otherwise, we need to repace the existing bean definition with a proxy factory bean that calls back to the original service bean.
            else {
                // First store the current service bean configuration under a different bean name.
                springConfig.addBeanConfiguration("${serviceName}MetricsLogged", beanConfig)

                // Now create the proxy factory bean and add the method interceptor to it.
                "$serviceName"(ProxyFactoryBean) {
                    // We don't want auto-detection of interfaces, otherwise Spring will just proxy the GroovyObject interface - not what we want!
                    autodetectInterfaces = false
                    targetName = "${serviceName}MetricsLogged"
                    interceptorNames = ["metricsLoggerMethodInterceptor"]
                }
            }
        }
    }
}

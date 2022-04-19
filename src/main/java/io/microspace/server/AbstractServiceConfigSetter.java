/*
 * MIT License
 *
 * Copyright (c) 2021 1619kHz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.microspace.server;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.function.Function;

import com.google.common.base.MoreObjects;

/**
 * @author i1619kHz
 */
abstract class AbstractServiceConfigSetter implements ServiceConfigSetter {
    private String defaultServiceName;
    private String defaultLogName;
    private Long requestTimeoutMillis;
    private Long maxRequestLength;
    private Boolean verboseResponses;
    private Function<? super HttpService, ? extends HttpService> decorator;

    @Override
    public ServiceConfigSetter decorator(Function<? super HttpService, ? extends HttpService> decorator) {
        this.decorator = requireNonNull(decorator, "decorator");
        return this;
    }

    @Override
    public ServiceConfigSetter requestTimeout(Duration requestTimeout) {
        return requestTimeoutMillis(requireNonNull(requestTimeout, "requestTimeout").toMillis());
    }

    @Override
    public ServiceConfigSetter requestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    @Override
    public ServiceConfigSetter maxRequestLength(long maxRequestLength) {
        this.maxRequestLength = maxRequestLength;
        return this;
    }

    @Override
    public ServiceConfigSetter verboseResponses(boolean verboseResponses) {
        this.verboseResponses = verboseResponses;
        return this;
    }

    @Override
    public ServiceConfigSetter defaultServiceName(String defaultServiceName) {
        this.defaultServiceName = defaultServiceName;
        return this;
    }

    @Override
    public ServiceConfigSetter defaultLogName(String defaultLogName) {
        this.defaultLogName = defaultLogName;
        return this;
    }

    ServiceConfigBuilder toServiceConfigBuilder(Route route, HttpService service) {
        final ServiceConfigBuilder serviceConfigBuilder = new ServiceConfigBuilder(route, service);
        AnnotatedService annotatedService = null;
        if (service instanceof AnnotatedService) {
            annotatedService = (AnnotatedService) service;
        }
        if (defaultServiceName != null) {
            serviceConfigBuilder.defaultServiceName(defaultServiceName);
        } else {
            if (annotatedService != null) {
                serviceConfigBuilder.defaultServiceName(annotatedService.serviceName());
            }
        }
        if (defaultLogName != null) {
            serviceConfigBuilder.defaultLogName(defaultLogName);
        } else {
            if (annotatedService != null) {
                serviceConfigBuilder.defaultLogName(annotatedService.methodName());
            }
        }
        if (requestTimeoutMillis != null) {
            serviceConfigBuilder.requestTimeoutMillis(requestTimeoutMillis);
        }
        if (maxRequestLength != null) {
            serviceConfigBuilder.maxRequestLength(maxRequestLength);
        }
        if (verboseResponses != null) {
            serviceConfigBuilder.verboseResponses(verboseResponses);
        }
        return serviceConfigBuilder;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("defaultServiceName", defaultServiceName)
                          .add("defaultLogName", defaultLogName)
                          .add("requestTimeoutMillis", requestTimeoutMillis)
                          .add("maxRequestLength", maxRequestLength)
                          .add("verboseResponses", verboseResponses)
                          .toString();
    }
}

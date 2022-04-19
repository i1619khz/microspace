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

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

/**
 * @author i1619kHz
 */
final class ServiceConfigBuilder implements ServiceConfigSetter {
    private final Route route;
    private final HttpService service;

    @Nullable
    private String defaultServiceName;
    @Nullable
    private String defaultLogName;
    @Nullable
    private Long requestTimeoutMillis;
    @Nullable
    private Long maxRequestLength;
    @Nullable
    private Boolean verboseResponses;

    ServiceConfigBuilder(Route route, HttpService service) {
        this.route = requireNonNull(route, "route");
        this.service = requireNonNull(service, "service");
    }

    @Override
    public ServiceConfigSetter decorator(Function<? super HttpService, ? extends HttpService> decorator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceConfigBuilder requestTimeout(Duration requestTimeout) {
        return requestTimeoutMillis(requestTimeout.toMillis());
    }

    @Override
    public ServiceConfigBuilder requestTimeoutMillis(long requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    @Override
    public ServiceConfigBuilder maxRequestLength(long maxRequestLength) {
        this.maxRequestLength = maxRequestLength;
        return this;
    }

    @Override
    public ServiceConfigBuilder verboseResponses(boolean verboseResponses) {
        this.verboseResponses = verboseResponses;
        return this;
    }

    @Override
    public ServiceConfigSetter defaultServiceName(String defaultServiceName) {
        this.defaultServiceName = requireNonNull(defaultServiceName, "defaultServiceName");
        return this;
    }

    @Override
    public ServiceConfigSetter defaultLogName(String defaultLogName) {
        this.defaultLogName = requireNonNull(defaultLogName, "defaultLogName");
        return this;
    }

    ServiceConfig build(long defaultRequestTimeoutMillis,
                        long defaultMaxRequestLength,
                        boolean defaultVerboseResponses) {
        return new ServiceConfig(
                route, service, defaultServiceName, defaultLogName,
                requestTimeoutMillis != null ? requestTimeoutMillis : defaultRequestTimeoutMillis,
                maxRequestLength != null ? maxRequestLength : defaultMaxRequestLength,
                verboseResponses != null ? verboseResponses : defaultVerboseResponses);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("route", route)
                          .add("service", service)
                          .add("defaultServiceName", defaultServiceName)
                          .add("defaultLogName", defaultLogName)
                          .add("requestTimeoutMillis", requestTimeoutMillis)
                          .add("maxRequestLength", maxRequestLength)
                          .add("verboseResponses", verboseResponses)
                          .toString();
    }
}

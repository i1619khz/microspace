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

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

/**
 * @author i1619kHz
 */
public final class ServiceConfig {
    private final Route route;
    private final HttpService service;

    private final String defaultServiceName;
    private final String defaultLogName;

    private final long requestTimeoutMillis;
    private final long maxRequestLength;

    private final boolean verboseResponses;
    private final boolean handlesCorsPreflight;

    ServiceConfig(Route route, HttpService service,
                  @Nullable String defaultServiceName, @Nullable String defaultLogName,
                  long requestTimeoutMillis, long maxRequestLength, boolean verboseResponses) {
        this.route = requireNonNull(route, "route");
        this.service = requireNonNull(service, "service");
        this.defaultServiceName = defaultServiceName;
        this.defaultLogName = defaultLogName;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.maxRequestLength = maxRequestLength;
        this.verboseResponses = verboseResponses;
        this.handlesCorsPreflight = false;
    }

    Route route() {
        return route;
    }

    HttpService service() {
        return service;
    }

    String defaultServiceName() {
        return defaultServiceName;
    }

    String defaultLogName() {
        return defaultLogName;
    }

    long requestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    long maxRequestLength() {
        return maxRequestLength;
    }

    boolean verboseResponses() {
        return verboseResponses;
    }

    boolean handlesCorsPreflight() {
        return handlesCorsPreflight;
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
                          .add("handlesCorsPreflight", handlesCorsPreflight)
                          .toString();
    }
}

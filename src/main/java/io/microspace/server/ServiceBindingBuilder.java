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

import java.time.Duration;
import java.util.function.Predicate;

/**
 * @author i1619kHz
 */
public final class ServiceBindingBuilder extends AbstractBindingBuilder {
    private final ServerBuilder serverBuilder;

    ServiceBindingBuilder(ServerBuilder serverBuilder) {
        this.serverBuilder = serverBuilder;
    }

    @Override
    public ServiceBindingBuilder path(String pathPattern) {
        return (ServiceBindingBuilder) super.path(pathPattern);
    }

    @Override
    public ServiceBindingBuilder pathPrefix(String prefix) {
        return (ServiceBindingBuilder) super.pathPrefix(prefix);
    }

    @Override
    public ServiceBindingBuilder get(String pathPattern) {
        return (ServiceBindingBuilder) super.get(pathPattern);
    }

    @Override
    public ServiceBindingBuilder post(String pathPattern) {
        return (ServiceBindingBuilder) super.post(pathPattern);
    }

    @Override
    public ServiceBindingBuilder put(String pathPattern) {
        return (ServiceBindingBuilder) super.put(pathPattern);
    }

    @Override
    public ServiceBindingBuilder patch(String pathPattern) {
        return (ServiceBindingBuilder) super.patch(pathPattern);
    }

    @Override
    public ServiceBindingBuilder delete(String pathPattern) {
        return (ServiceBindingBuilder) super.delete(pathPattern);
    }

    @Override
    public ServiceBindingBuilder options(String pathPattern) {
        return (ServiceBindingBuilder) super.options(pathPattern);
    }

    @Override
    public ServiceBindingBuilder head(String pathPattern) {
        return (ServiceBindingBuilder) super.head(pathPattern);
    }

    @Override
    public ServiceBindingBuilder trace(String pathPattern) {
        return (ServiceBindingBuilder) super.trace(pathPattern);
    }

    @Override
    public ServiceBindingBuilder connect(String pathPattern) {
        return (ServiceBindingBuilder) super.connect(pathPattern);
    }

    @Override
    public ServiceBindingBuilder methods(HttpMethod... methods) {
        return (ServiceBindingBuilder) super.methods(methods);
    }

    @Override
    public ServiceBindingBuilder methods(Iterable<HttpMethod> methods) {
        return (ServiceBindingBuilder) super.methods(methods);
    }

    @Override
    public ServiceBindingBuilder consumes(MediaType... consumeTypes) {
        return (ServiceBindingBuilder) super.consumes(consumeTypes);
    }

    @Override
    public ServiceBindingBuilder consumes(Iterable<MediaType> consumeTypes) {
        return (ServiceBindingBuilder) super.consumes(consumeTypes);
    }

    @Override
    public ServiceBindingBuilder produces(MediaType... produceTypes) {
        return (ServiceBindingBuilder) super.produces(produceTypes);
    }

    @Override
    public ServiceBindingBuilder produces(Iterable<MediaType> produceTypes) {
        return (ServiceBindingBuilder) super.produces(produceTypes);
    }

    @Override
    public ServiceBindingBuilder matchesParams(String... paramPredicates) {
        return (ServiceBindingBuilder) super.matchesParams(paramPredicates);
    }

    @Override
    public ServiceBindingBuilder matchesParams(Iterable<String> paramPredicates) {
        return (ServiceBindingBuilder) super.matchesParams(paramPredicates);
    }

    @Override
    public ServiceBindingBuilder matchesParams(String paramName, Predicate<? super String> valuePredicate) {
        return (ServiceBindingBuilder) super.matchesParams(paramName, valuePredicate);
    }

    @Override
    public ServiceBindingBuilder matchesHeaders(String... headerPredicates) {
        return (ServiceBindingBuilder) super.matchesHeaders(headerPredicates);
    }

    @Override
    public ServiceBindingBuilder matchesHeaders(Iterable<String> headerPredicates) {
        return (ServiceBindingBuilder) super.matchesHeaders(headerPredicates);
    }

    @Override
    public ServiceBindingBuilder matchesHeaders(CharSequence headerName,
                                                Predicate<? super String> valuePredicate) {
        return (ServiceBindingBuilder) super.matchesHeaders(headerName, valuePredicate);
    }

    @Override
    public ServiceBindingBuilder route(Route route) {
        return (ServiceBindingBuilder) super.route(route);
    }

    @Override
    public ServiceBindingBuilder requestTimeout(Duration requestTimeout) {
        return (ServiceBindingBuilder) super.requestTimeout(requestTimeout);
    }

    @Override
    public ServiceBindingBuilder requestTimeoutMillis(long requestTimeoutMillis) {
        return (ServiceBindingBuilder) super.requestTimeoutMillis(requestTimeoutMillis);
    }

    @Override
    public ServiceBindingBuilder maxRequestLength(long maxRequestLength) {
        return (ServiceBindingBuilder) super.maxRequestLength(maxRequestLength);
    }

    @Override
    public ServiceBindingBuilder verboseResponses(boolean verboseResponses) {
        return (ServiceBindingBuilder) super.verboseResponses(verboseResponses);
    }

    @Override
    public ServiceBindingBuilder defaultServiceName(String defaultServiceName) {
        return (ServiceBindingBuilder) super.defaultServiceName(defaultServiceName);
    }

    @Override
    public ServiceBindingBuilder defaultLogName(String defaultLogName) {
        return (ServiceBindingBuilder) super.defaultLogName(defaultLogName);
    }

    public ServerBuilder build(HttpService httpService) {
        super.httpService(httpService);
        final Route route = buildRoute();
        return serviceConfigBuilder(toServiceConfigBuilder(route, httpService()));
    }

    ServerBuilder serviceConfigBuilder(ServiceConfigBuilder serviceConfigBuilder) {
        return this.serverBuilder.serviceConfigBuilder(serviceConfigBuilder);
    }
}

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
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.microspace.server.annotation.RequestConverterFunction;
import io.microspace.server.annotation.ResponseConverterFunction;

/**
 * @author i1619kHz
 */
final class AnnotatedServiceBindingBuilder extends AbstractServiceConfigSetter {
    private final Builder<ExceptionHandlerFunction> exceptionHandlerFunctionBuilder = ImmutableList.builder();
    private final Builder<RequestConverterFunction> requestConverterFunctionBuilder = ImmutableList.builder();
    private final Builder<ResponseConverterFunction> responseConverterFunctionBuilder = ImmutableList.builder();
    private final ServerBuilder serverBuilder;
    private String pathPrefix = "/";
    private Object service;

    public AnnotatedServiceBindingBuilder(ServerBuilder serverBuilder) {
        this.serverBuilder = serverBuilder;
    }

    public AnnotatedServiceBindingBuilder pathPrefix(String pathPrefix) {
        this.pathPrefix = requireNonNull(pathPrefix, "pathPrefix");
        return this;
    }

    public AnnotatedServiceBindingBuilder exceptionHandlers(
            ExceptionHandlerFunction... exceptionHandlerFunctions) {
        requireNonNull(exceptionHandlerFunctions, "exceptionHandlerFunctions");
        exceptionHandlerFunctionBuilder.add(exceptionHandlerFunctions);
        return this;
    }

    public AnnotatedServiceBindingBuilder exceptionHandlers(
            Iterable<? extends ExceptionHandlerFunction> exceptionHandlerFunctions) {
        requireNonNull(exceptionHandlerFunctions, "exceptionHandlerFunctions");
        exceptionHandlerFunctionBuilder.addAll(exceptionHandlerFunctions);
        return this;
    }

    public AnnotatedServiceBindingBuilder responseConverters(
            ResponseConverterFunction... responseConverterFunctions) {
        requireNonNull(responseConverterFunctions, "responseConverterFunctions");
        responseConverterFunctionBuilder.add(responseConverterFunctions);
        return this;
    }

    public AnnotatedServiceBindingBuilder responseConverters(
            Iterable<? extends ResponseConverterFunction> responseConverterFunctions) {
        requireNonNull(responseConverterFunctions, "responseConverterFunctions");
        responseConverterFunctionBuilder.addAll(responseConverterFunctions);
        return this;
    }

    public AnnotatedServiceBindingBuilder requestConverters(
            RequestConverterFunction... requestConverterFunctions) {
        requireNonNull(requestConverterFunctions, "requestConverterFunctions");
        requestConverterFunctionBuilder.add(requestConverterFunctions);
        return this;
    }

    public AnnotatedServiceBindingBuilder requestConverters(
            Iterable<? extends RequestConverterFunction> requestConverterFunctions) {
        requireNonNull(requestConverterFunctions, "requestConverterFunctions");
        requestConverterFunctionBuilder.addAll(requestConverterFunctions);
        return this;
    }

    public ServerBuilder build(Object service) {
        requireNonNull(service, "service");
        this.service = service;
        serverBuilder.serviceConfigBuilder(this);
        return serverBuilder;
    }

    @Override
    public AnnotatedServiceBindingBuilder decorator(
            Function<? super HttpService, ? extends HttpService> decorator) {
        super.decorator(decorator);
        return this;
    }

    @Override
    public AnnotatedServiceBindingBuilder requestTimeout(Duration requestTimeout) {
        super.requestTimeout(requestTimeout);
        return this;
    }

    @Override
    public ServiceConfigSetter requestTimeoutMillis(long requestTimeoutMillis) {
        super.requestTimeoutMillis(requestTimeoutMillis);
        return this;
    }

    @Override
    public AnnotatedServiceBindingBuilder maxRequestLength(long maxRequestLength) {
        super.maxRequestLength(maxRequestLength);
        return this;
    }

    @Override
    public AnnotatedServiceBindingBuilder verboseResponses(boolean verboseResponses) {
        super.verboseResponses(verboseResponses);
        return this;
    }

    @Override
    public AnnotatedServiceBindingBuilder defaultServiceName(String defaultServiceName) {
        super.defaultServiceName(defaultServiceName);
        return this;
    }

    @Override
    public AnnotatedServiceBindingBuilder defaultLogName(String defaultLogName) {
        super.defaultLogName(defaultLogName);
        return this;
    }

    @Override
    ServiceConfigBuilder toServiceConfigBuilder(Route route, HttpService service) {
        return super.toServiceConfigBuilder(route, service);
    }

    List<ServiceConfigBuilder> buildServiceConfigBuilder() {
        List<RequestConverterFunction> requestConverterFunctions
                = requestConverterFunctionBuilder.build();
        List<ResponseConverterFunction> responseConverterFunctions
                = responseConverterFunctionBuilder.build();
        List<ExceptionHandlerFunction> exceptionHandlerFunctions =
                exceptionHandlerFunctionBuilder.build();

        List<AnnotatedServiceElement> elements = AnnotatedServiceFactory.find(
                pathPrefix, service, requestConverterFunctions,
                responseConverterFunctions, exceptionHandlerFunctions);

        return elements.stream().map(element -> {
            return toServiceConfigBuilder(element.route(), element.service());
        }).collect(ImmutableList.toImmutableList());
    }
}

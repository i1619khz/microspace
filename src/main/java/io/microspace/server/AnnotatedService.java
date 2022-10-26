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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import com.google.common.base.MoreObjects;

import io.microspace.internal.AnnotationUtil;
import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.microspace.server.annotation.RequestConverterFunction;
import io.microspace.server.annotation.ResponseConverterFunction;
import io.microspace.server.annotation.ServiceName;

/**
 * @author i1619kHz
 */
final class AnnotatedService implements HttpService {
    private final Object target;
    private final Method method;
    private final boolean needToUseBlockingTaskExecutor;
    private final Map<String, Set<String>> addedHeaders;
    private final List<RequestConverterFunction> requestConverterFunctions;
    private final List<ResponseConverterFunction> responseConverterFunctions;
    private final List<ExceptionHandlerFunction> exceptionHandlerFunctions;
    private final String defaultServiceName;
    private final ResponseType responseType;

    AnnotatedService(Object target, Method method,
                     boolean needToUseBlockingTaskExecutor,
                     Map<String, Set<String>> addedHeaders,
                     List<RequestConverterFunction> requestConverterFunctions,
                     List<ResponseConverterFunction> responseConverterFunctions,
                     List<ExceptionHandlerFunction> exceptionHandlerFunctions) {
        this.target = requireNonNull(target, "target");
        this.method = requireNonNull(method, "method");
        this.addedHeaders = requireNonNull(addedHeaders, "addedHeaders");
        this.needToUseBlockingTaskExecutor = needToUseBlockingTaskExecutor;
        this.requestConverterFunctions = requireNonNull(requestConverterFunctions, "requestConverterFunctions");
        this.responseConverterFunctions = requireNonNull(responseConverterFunctions,
                                                         "responseConverterFunctions");
        this.exceptionHandlerFunctions = requireNonNull(exceptionHandlerFunctions, "exceptionHandlerFunctions");
        final Class<?> returnType = method.getReturnType();
        if (returnType.isAssignableFrom(HttpResponse.class)) {
            responseType = ResponseType.HTTP_RESPONSE;
        } else if (returnType.isAssignableFrom(CompletionStage.class)) {
            responseType = ResponseType.COMPLETION_STAGE;
        } else {
            responseType = ResponseType.OTHER_OBJECTS;
        }

        ServiceName serviceName = AnnotationUtil.findFirst(method, ServiceName.class);
        if (serviceName == null) {
            serviceName = AnnotationUtil.findFirst(target.getClass(), ServiceName.class);
        }
        if (serviceName != null) {
            defaultServiceName = serviceName.value();
        } else {
            defaultServiceName = target.getClass().getName();
        }

        this.method.setAccessible(true);
    }

    public String serviceName() {
        return defaultServiceName;
    }

    public String methodName() {
        return method.getName();
    }

    Object object() {
        return target;
    }

    Method method() {
        return method;
    }

    @Override
    public HttpResponse serve(Request request) {
        try {
            Object invoke = method.invoke(target);
            return HttpResponse.of((String) invoke);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return HttpResponse.of("");
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("target", target)
                          .add("method", method)
                          .toString();
    }

    /**
     * Response type classification of the annotated {@link Method}.
     */
    private enum ResponseType {
        HTTP_RESPONSE, COMPLETION_STAGE, OTHER_OBJECTS
    }
}

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import io.micrometer.core.instrument.MeterRegistry;
import io.microspace.internal.banner.BannerPrinter;
import io.microspace.server.annotation.ExceptionHandlerFunction;
import io.netty.channel.ChannelOption;

/**
 * @author i1619kHz
 */
public final class ServerConfig {
    private final MeterRegistry meterRegistry;
    private final Map<ChannelOption<?>, Object> channelOptions;
    private final Map<ChannelOption<?>, Object> childChannelOptions;
    private final ExecutorService startStopExecutor;
    private final BannerPrinter banner;
    private final Map<Class<? extends Throwable>, ExceptionHandlerFunction> exceptionServices;
    private final boolean useSsl;
    private final boolean useEpoll;
    private final boolean useSession;
    private final boolean useIoUsing;
    private final boolean shutdownWorkerGroupOnStop;
    private final String bannerText;
    private final String bannerFont;
    private final String sessionKey;
    private final String viewSuffix;
    private final String templateFolder;
    private final String serverThreadName;
    private final String profiles;
    private final List<ServerPort> ports;
    private final int maxNumConnections;
    private final int http2InitialConnectionWindowSize;
    private final int http2InitialStreamWindowSize;
    private final int http2MaxFrameSize;
    private final int http1MaxInitialLineLength;
    private final int http1MaxHeaderSize;
    private final int http1MaxChunkSize;
    private final int acceptThreadCount;
    private final int ioThreadCount;
    private final int serverRestartCount;
    private final long idleTimeoutMillis;
    private final long pingIntervalMillis;
    private final long maxConnectionAgeMillis;
    private final long http2MaxHeaderListSize;
    private final long http2MaxStreamsPerConnection;
    private final Duration gracefulShutdownQuietPeriod;
    private final Duration gracefulShutdownTimeout;
    private final Class<?> bootCls;
    private final String[] args;
    private final List<ServiceConfig> serviceConfigs;

    ServerConfig(List<ServiceConfig> serviceConfigs,
                 Map<Class<? extends Throwable>, ExceptionHandlerFunction> exceptionServices,
                 MeterRegistry meterRegistry, Class<?> bootCls, String[] args, BannerPrinter banner,
                 Map<ChannelOption<?>, Object> channelOptions,
                 Map<ChannelOption<?>, Object> childChannelOptions,
                 boolean useSsl, boolean useEpoll, boolean shutdownWorkerGroupOnStop,
                 ExecutorService startStopExecutor,
                 String bannerText, String bannerFont, String sessionKey, String viewSuffix,
                 String templateFolder, String serverThreadName, String profiles,
                 boolean useSession, boolean useIoUsing, List<ServerPort> ports,
                 int maxNumConnections, int http2InitialConnectionWindowSize, int http2InitialStreamWindowSize,
                 int http2MaxFrameSize, int http1MaxInitialLineLength, int http1MaxHeaderSize,
                 int http1MaxChunkSize, long idleTimeoutMillis, long pingIntervalMillis,
                 long maxConnectionAgeMillis, long http2MaxHeaderListSize, long http2MaxStreamsPerConnection,
                 int acceptThreadCount, int ioThreadCount, int serverRestartCount,
                 Duration gracefulShutdownQuietPeriod,
                 Duration gracefulShutdownTimeout) {
        this.serviceConfigs = serviceConfigs;
        this.exceptionServices = exceptionServices;
        this.meterRegistry = meterRegistry;
        this.bootCls = bootCls;
        this.args = args;
        this.banner = banner;
        this.channelOptions = channelOptions;
        this.childChannelOptions = childChannelOptions;
        this.useSsl = useSsl;
        this.useEpoll = useEpoll;
        this.shutdownWorkerGroupOnStop = shutdownWorkerGroupOnStop;
        this.startStopExecutor = startStopExecutor;
        this.bannerText = bannerText;
        this.bannerFont = bannerFont;
        this.sessionKey = sessionKey;
        this.viewSuffix = viewSuffix;
        this.templateFolder = templateFolder;
        this.serverThreadName = serverThreadName;
        this.profiles = profiles;
        this.useSession = useSession;
        this.useIoUsing = useIoUsing;
        this.ports = ports;
        this.maxNumConnections = maxNumConnections;
        this.http2InitialConnectionWindowSize = http2InitialConnectionWindowSize;
        this.http2InitialStreamWindowSize = http2InitialStreamWindowSize;
        this.http2MaxFrameSize = http2MaxFrameSize;
        this.http1MaxInitialLineLength = http1MaxInitialLineLength;
        this.http1MaxHeaderSize = http1MaxHeaderSize;
        this.http1MaxChunkSize = http1MaxChunkSize;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.pingIntervalMillis = pingIntervalMillis;
        this.maxConnectionAgeMillis = maxConnectionAgeMillis;
        this.http2MaxHeaderListSize = http2MaxHeaderListSize;
        this.http2MaxStreamsPerConnection = http2MaxStreamsPerConnection;
        this.acceptThreadCount = acceptThreadCount;
        this.ioThreadCount = ioThreadCount;
        this.serverRestartCount = serverRestartCount;
        this.gracefulShutdownQuietPeriod = gracefulShutdownQuietPeriod;
        this.gracefulShutdownTimeout = gracefulShutdownTimeout;
    }

    public Class<?> bootCls() {
        return bootCls;
    }

    public String[] args() {
        return args;
    }

    public Map<ChannelOption<?>, Object> channelOptions() {
        return channelOptions;
    }

    public Map<ChannelOption<?>, Object> childChannelOptions() {
        return childChannelOptions;
    }

    public boolean useSsl() {
        return useSsl;
    }

    public boolean useEpoll() {
        return useEpoll;
    }

    public String bannerText() {
        return bannerText;
    }

    public String bannerFont() {
        return bannerFont;
    }

    public String sessionKey() {
        return sessionKey;
    }

    public String viewSuffix() {
        return viewSuffix;
    }

    public String templateFolder() {
        return templateFolder;
    }

    public String serverThreadName() {
        return serverThreadName;
    }

    public String profiles() {
        return profiles;
    }

    public boolean useSession() {
        return useSession;
    }

    public List<ServerPort> ports() {
        return ports;
    }

    public int maxNumConnections() {
        return maxNumConnections;
    }

    public int http2InitialConnectionWindowSize() {
        return http2InitialConnectionWindowSize;
    }

    public int http2InitialStreamWindowSize() {
        return http2InitialStreamWindowSize;
    }

    public int http2MaxFrameSize() {
        return http2MaxFrameSize;
    }

    public int http1MaxInitialLineLength() {
        return http1MaxInitialLineLength;
    }

    public int http1MaxHeaderSize() {
        return http1MaxHeaderSize;
    }

    public int http1MaxChunkSize() {
        return http1MaxChunkSize;
    }

    public long idleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public long pingIntervalMillis() {
        return pingIntervalMillis;
    }

    public long maxConnectionAgeMillis() {
        return maxConnectionAgeMillis;
    }

    public long http2MaxHeaderListSize() {
        return http2MaxHeaderListSize;
    }

    public long http2MaxStreamsPerConnection() {
        return http2MaxStreamsPerConnection;
    }

    public int acceptThreadCount() {
        return acceptThreadCount;
    }

    public int ioThreadCount() {
        return ioThreadCount;
    }

    public BannerPrinter banner() {
        return banner;
    }

    public int serverRestartCount() {
        return serverRestartCount;
    }

    public ExecutorService executorService() {
        return startStopExecutor;
    }

    public MeterRegistry meterRegistry() {
        return meterRegistry;
    }

    public Duration gracefulShutdownQuietPeriod() {
        return gracefulShutdownQuietPeriod;
    }

    public Duration gracefulShutdownTimeout() {
        return gracefulShutdownTimeout;
    }

    public Map<Class<? extends Throwable>, ExceptionHandlerFunction> exceptionServices() {
        return exceptionServices;
    }

    public List<ServiceConfig> serviceConfigs() {
        return serviceConfigs;
    }

    public boolean shutdownWorkerGroupOnStop() {
        return shutdownWorkerGroupOnStop;
    }

    public boolean useIoUsing() {
        return useIoUsing;
    }

    static Duration validateNonNegative(Duration duration, String fieldName) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + ": " + duration + " (expected: >= 0)");
        }
        return duration;
    }

    static void validateGreaterThanOrEqual(Duration larger, String largerFieldName,
                                           Duration smaller, String smallerFieldName) {
        if (larger.compareTo(smaller) < 0) {
            throw new IllegalArgumentException(largerFieldName + " must be greater than or equal to" +
                                               smallerFieldName);
        }
    }
}

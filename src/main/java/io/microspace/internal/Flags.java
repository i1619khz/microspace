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
package io.microspace.internal;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

import io.microspace.context.banner.BannerFont;
import io.microspace.server.HttpMethod;
import io.microspace.server.ServerPort;
import io.microspace.server.SessionProtocol;
import io.microspace.server.annotation.Delete;
import io.microspace.server.annotation.Get;
import io.microspace.server.annotation.Head;
import io.microspace.server.annotation.Options;
import io.microspace.server.annotation.Patch;
import io.microspace.server.annotation.Post;
import io.microspace.server.annotation.Put;
import io.microspace.server.annotation.Trace;

/**
 * @author i1619kHz
 */
public final class Flags {
    private static final Pattern PROPERTIES_REGEX = Pattern.compile(".*\\.properties");
    private static final Pattern YML_REGEX = Pattern.compile(".*\\.yml");
    private static final String HTTP_URL_PREFIX = "http://localhost";

    private static final int DEFAULT_MAX_CONNECTION_COUNT = Integer.MAX_VALUE;
    private static final int DEFAULT_HTTP2_INITIAL_CONNECTION_WINDOWS_SIZE = 0;
    private static final int DEFAULT_HTTP2_INITIAL_STREAM_WINDOW_SIZE = 0;
    private static final int DEFAULT_HTTP2_MAX_FRAME_SIZE = 0;
    private static final int DEFAULT_HTTP1_MAX_INITIAL_LINE_LENGTH = 0;
    private static final int DEFAULT_HTTP1_MAX_HEADER_SIZE = 0;
    private static final int DEFAULT_HTTP1_MAX_CHUNK_SIZE = 0;

    private static final long DEFAULT_IDLE_TIMEOUT_MILLIS = 0;
    private static final long DEFAULT_PING_INTERVAL_MILLIS = 0;
    private static final long DEFAULT_MAX_CONNECTION_AGE_MILLIS = 0;
    private static final long DEFAULT_HTTP2_MAX_HEADER_LIST_SIZE = 0;
    private static final long DEFAULT_HTTP2_MAX_STREAMS_PER_CONNECTION = 0;
    private static final long STOP_QUIET_PERIOD = 2L;
    private static final long STOP_TIMEOUT = 15L;
    private static final boolean SHUTDOWN_WORKER_GROUP_ON_STOP = true;

    private static final String MICROSPACE_FRAMEWORK = " :: Microspace Framework :: ";
    private static final String SERVER_THREAD_NAME = "（'-'*)";
    private static final String BANNER_TEXT = "microspace";
    private static final String SESSION_KEY = "MSPSESSION";
    private static final String VIEW_SUFFIX = ".html";
    private static final String TEMPLATE_FOLDER = "/templates";
    private static final String BANNER_FONT = BannerFont.FONT_DEFAULT;
    private static final String DEFAULT_PROFILES = "";

    private static final boolean SESSION_ENABLE = false;
    private static final boolean USE_SSL = false;
    private static final boolean USE_EPOLL = Epolls.epollIsAvailable();
    private static final int DEFAULT_PORT = 8080;

    private static final String SSL_CERT = "";
    private static final String SSL_PRIVATE_KEY = "";
    private static final String SSL_PRIVATE_KEY_PASS = "";

    private static final int MAX_CONNECTION_COUNT = DEFAULT_MAX_CONNECTION_COUNT;
    private static final int ACCEPT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int IO_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;
    private static final ServerPort DEFAULT_SERVER_PORT = new ServerPort(Flags.defaultPort(),
                                                                         SessionProtocol.HTTP);
    /**
     * Mapping from HTTP method annotation to {@link HttpMethod}, like following.
     */
     public static final Map<Class<?>, HttpMethod> HTTP_METHOD_MAP =
            ImmutableMap.<Class<?>, HttpMethod>builder()
                        .put(Options.class, HttpMethod.OPTIONS)
                        .put(Get.class, HttpMethod.GET)
                        .put(Head.class, HttpMethod.HEAD)
                        .put(Post.class, HttpMethod.POST)
                        .put(Put.class, HttpMethod.PUT)
                        .put(Patch.class, HttpMethod.PATCH)
                        .put(Delete.class, HttpMethod.DELETE)
                        .put(Trace.class, HttpMethod.TRACE)
                        .build();

    public static boolean checkPort(int serverPort) {
        return serverPort > 0 && serverPort <= 65533;
    }

    public static ServerPort defaultServerPort() {
        return DEFAULT_SERVER_PORT;
    }

    public static Pattern propertiesRegex() {
        return PROPERTIES_REGEX;
    }

    public static Pattern ymlRegex() {
        return YML_REGEX;
    }

    public static String defaultBannerText() {
        return BANNER_TEXT;
    }

    public static String defaultBannerFont() {
        return BANNER_FONT;
    }

    public static String defaultSessionKey() {
        return SESSION_KEY;
    }

    public static String defaultViewSuffix() {
        return VIEW_SUFFIX;
    }

    public static String defaultTemplateFolder() {
        return TEMPLATE_FOLDER;
    }

    public static String microspaceFramework() {
        return MICROSPACE_FRAMEWORK;
    }

    public static String defaultServerThreadName() {
        return SERVER_THREAD_NAME;
    }

    public static boolean useSession() {
        return SESSION_ENABLE;
    }

    public static String profiles() {
        return DEFAULT_PROFILES;
    }

    public static int defaultPort() {
        return DEFAULT_PORT;
    }

    public static boolean useSsl() {
        return USE_SSL;
    }

    public static boolean useEpoll() {
        return USE_EPOLL;
    }

    public static int defaultMaxNumConnections() {
        return MAX_CONNECTION_COUNT;
    }

    public static int defaultHttp2InitialConnectionWindowSize() {
        return 0;
    }

    public static int defaultHttp2InitialStreamWindowSize() {
        return 0;
    }

    public static int defaultHttp2MaxFrameSize() {
        return 0;
    }

    public static int defaultHttp1MaxInitialLineLength() {
        return 0;
    }

    public static int defaultHttp1MaxHeaderSize() {
        return 0;
    }

    public static int defaultHttp1MaxChunkSize() {
        return 0;
    }

    public static long defaultIdleTimeoutMillis() {
        return 0;
    }

    public static long defaultPingIntervalMillis() {
        return 0;
    }

    public static long defaultMaxConnectionAgeMillis() {
        return 0;
    }

    public static long defaultHttp2MaxHeaderListSize() {
        return 0;
    }

    public static long defaultHttp2MaxStreamsPerConnection() {
        return 0;
    }

    public static int defaultAcceptThreadCount() {
        return ACCEPT_THREAD_COUNT;
    }

    public static int defaultIoThreadCount() {
        return IO_THREAD_COUNT;
    }

    public static int defaultServerRestartCount() {
        return 3;
    }

    public static String httpUrlPrefix() {
        return HTTP_URL_PREFIX;
    }

    public static long defaultStopQuietPeriod() {
        return STOP_QUIET_PERIOD;
    }

    public static long defaultStopTimeout() {
        return STOP_TIMEOUT;
    }

    public static boolean defaultShutdownWorkerGroupOnStop() {
        return SHUTDOWN_WORKER_GROUP_ON_STOP;
    }

    public static boolean validateHeaders() {
        return true;
    }
}

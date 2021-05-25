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

import io.microspace.context.banner.BannerFont;
import io.microspace.server.ServerPort;
import io.microspace.server.SessionProtocol;

import java.util.regex.Pattern;

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
    private static final ServerPort DEFAULT_SERVER_PORT = new ServerPort(Flags.defaultPort(), SessionProtocol.HTTP);

    public static ServerPort defaultServerPort() {
        return DEFAULT_SERVER_PORT;
    }

    public static Pattern propertiesRegex() {
        return PROPERTIES_REGEX;
    }

    public static Pattern ymlRegex() {
        return YML_REGEX;
    }

    public static String bannerText() {
        return BANNER_TEXT;
    }

    public static String bannerFont() {
        return BANNER_FONT;
    }

    public static String sessionKey() {
        return SESSION_KEY;
    }

    public static String viewSuffix() {
        return VIEW_SUFFIX;
    }

    public static String templateFolder() {
        return TEMPLATE_FOLDER;
    }

    public static String microspaceFramework() {
        return MICROSPACE_FRAMEWORK;
    }

    public static String serverThreadName() {
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

    public static int maxNumConnections() {
        return MAX_CONNECTION_COUNT;
    }

    public static int http2InitialConnectionWindowSize() {
        return 0;
    }

    public static int http2InitialStreamWindowSize() {
        return 0;
    }

    public static int http2MaxFrameSize() {
        return 0;
    }

    public static int http1MaxInitialLineLength() {
        return 0;
    }

    public static int http1MaxHeaderSize() {
        return 0;
    }

    public static int http1MaxChunkSize() {
        return 0;
    }

    public static long idleTimeoutMillis() {
        return 0;
    }

    public static long pingIntervalMillis() {
        return 0;
    }

    public static long maxConnectionAgeMillis() {
        return 0;
    }

    public static long http2MaxHeaderListSize() {
        return 0;
    }

    public static long http2MaxStreamsPerConnection() {
        return 0;
    }

    public static int acceptThreadCount() {
        return ACCEPT_THREAD_COUNT;
    }

    public static int ioThreadCount() {
        return IO_THREAD_COUNT;
    }

    public static int serverRestartCount() {
        return 3;
    }

    public static String httpUrlPrefix() {
        return HTTP_URL_PREFIX;
    }
}

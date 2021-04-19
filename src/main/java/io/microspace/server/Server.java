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

import io.microspace.context.banner.Banner;
import io.microspace.context.banner.DefaultApplicationBanner;
import io.microspace.core.EventLoopGroups;
import io.microspace.core.ThreadFactories;
import io.microspace.core.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author i1619kHz
 */
public final class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  /** Service startup status, using volatile to ensure threads are visible. */
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Banner defaultBanner = new DefaultApplicationBanner();
  private final ServerSpec spec;
  private SslContext sslContext;

  /**
   * Create {@link ServerBuilder}
   *
   * @return ServerBuilder
   */
  public static ServerBuilder builder() {
    return new ServerBuilder();
  }

  /**
   * Create a {@link Server} through {@link ServerSpec}
   *
   * @param spec Server spec config
   */
  public Server(ServerSpec spec) {
    this.spec = spec;
  }

  public void start() {
    try {
      this.printlnBanner();
      this.startBefore();
    } catch (CertificateException | SSLException e) {
      logger.error("", e);
    }
  }

  private void startBefore() throws CertificateException, SSLException {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    final SelfSignedCertificate ssc = new SelfSignedCertificate();

    if (!spec.useSsl()) {
      this.sslContext = null;
    }

    final String sslCert = spec.sslCert();
    final String sslPrivateKey = spec.sslPrivateKey();
    final String sslPrivateKeyPass = spec.sslPrivateKeyPass();

    this.sslContext = SslContextBuilder.forServer(setKeyCertFileAndPriKey(sslCert, ssc.certificate()),
        setKeyCertFileAndPriKey(sslPrivateKey, ssc.privateKey()), sslPrivateKeyPass).build();

    this.bindServerToHost(spec.serverPort()).addListener(f -> {
      if (!f.isSuccess()) {
        future.completeExceptionally(f.cause());
      } else {
        this.running.set(true);
        logger.info("Serving HTTP at {}", spec.serverPort().localAddress());
      }
    });
  }

  private ChannelFuture bindServerToHost(ServerPort port) {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

    final ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.childHandler(new Http2ServerInitializer(sslContext));
    serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    serverBootstrap.handler(new ConnectionLimitHandler(spec.maxNumConnections()));

    this.spec.channelOptions().forEach((k, v) -> {
      @SuppressWarnings("unchecked")
      final ChannelOption<Object> castOption = (ChannelOption<Object>) k;
      serverBootstrap.option(castOption, v);
    });

    this.spec.childChannelOptions().forEach((k, v) -> {
      @SuppressWarnings("unchecked")
      final ChannelOption<Object> castOption = (ChannelOption<Object>) k;
      serverBootstrap.childOption(castOption, v);
    });

    final EventLoopGroup bossGroup = EventLoopGroups.newEventLoopGroup(spec.acceptThreadCount(),
        ThreadFactories.newEventLoopThreadFactory(bossThreadName(spec.serverPort(),
            "boss"), false));

    final EventLoopGroup workerGroup = EventLoopGroups.newEventLoopGroup(spec.ioThreadCount(),
        ThreadFactories.newEventLoopThreadFactory(bossThreadName(spec.serverPort(),
            "worker"), true));

    serverBootstrap.group(bossGroup, workerGroup);
    serverBootstrap.channel(TransportType.detectTransportType().serverChannelType());
    return serverBootstrap.bind(port.localAddress());
  }

  /**
   * print default banner
   */
  private void printlnBanner() {
    this.defaultBanner.printBanner(System.out, spec.bannerText(), spec.bannerFont());
  }

  /**
   * Use the configured path if the certificate and private key are
   * configured, otherwise use the default configuration
   *
   * @param keyPath         private keystore path
   * @param defaultFilePath default private keystore path
   * @return keystore file
   */
  private File setKeyCertFileAndPriKey(String keyPath, File defaultFilePath) {
    return Objects.nonNull(keyPath) ? Paths.get(keyPath).toFile() : defaultFilePath;
  }

  private static String bossThreadName(ServerPort port, String prefix) {
    final InetSocketAddress localAddr = port.localAddress();
    final String localHostName =
        localAddr.getAddress().isAnyLocalAddress() ? "*" : localAddr.getHostString();

    // e.g. 'armeria-boss-http-*:8080'
    //      'armeria-boss-http-127.0.0.1:8443'
    //      'armeria-boss-proxy+http+https-127.0.0.1:8443'
    final String protocolNames = port.protocols().stream()
        .map(SessionProtocol::uriText)
        .collect(Collectors.joining("+"));
    return "microspace-" + prefix + "-" + protocolNames + '-' + localHostName + ':' + localAddr.getPort();
  }
}

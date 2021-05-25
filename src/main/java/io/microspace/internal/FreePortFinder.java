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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author i1619kHz
 */
public final class FreePortFinder {
    /**
     * The minimum server currentMinPort number for IPv4.
     * Set at 1100 to avoid returning privileged currentMinPort numbers.
     */
    public static final int MIN_PORT_NUMBER = 1100;

    /**
     * The maximum server currentMinPort number for IPv4.
     */
    public static final int MAX_PORT_NUMBER = 65535;

    /**
     * We'll hold open the lowest port in this process
     * so parallel processes won't use the same block
     * of ports.   They'll go up to the next block.
     */
    private static final ServerSocket LOCK;

    /**
     * Incremented to the next lowest available port when findFreeLocalPort() is called.
     */
    private static final AtomicInteger currentMinPort = new AtomicInteger(MIN_PORT_NUMBER);

    /**
     * Creates a new instance.
     */
    private FreePortFinder() {
        // Do nothing
    }

    static {
        int port = MIN_PORT_NUMBER;
        ServerSocket ss = null;

        while (ss == null) {
            try {
                ss = new ServerSocket(port);
            } catch (Exception e) {
                port += 200;
            }
        }
        LOCK = ss;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    LOCK.close();
                } catch (Exception ex) {
                    //ignore
                }
            }
        });
        currentMinPort.set(port + 1);
    }

    /**
     * Gets the next available port starting at the lowest number. This is the preferred
     * method to use. The port return is immediately marked in use and doesn't rely on the caller actually opening
     * the port.
     *
     * @return the available port
     * @throws IllegalArgumentException is thrown if the port number is out of range
     * @throws NoSuchElementException   if there are no ports available
     */
    public static synchronized int findFreeLocalPort() {
        return findFreeLocalPort(null);
    }

    /**
     * Gets the next available port starting at the lowest number. This is the preferred
     * method to use. The port return is immediately marked in use and doesn't rely on the caller actually opening
     * the port.
     *
     * @param bindAddress the address that will try to bind
     * @return the available port
     * @throws IllegalArgumentException is thrown if the port number is out of range
     * @throws NoSuchElementException   if there are no ports available
     */
    public static synchronized int findFreeLocalPort(InetAddress bindAddress) {
        int next = findFreeLocalPort(currentMinPort.get(), bindAddress);
        currentMinPort.set(next + 1);
        return next;
    }

    /**
     * Gets the next available port starting at the lowest number. This is the preferred
     * method to use. The port return is immediately marked in use and doesn't rely on the caller actually opening
     * the port.
     *
     * @return the available port
     * @throws IllegalArgumentException is thrown if the port number is out of range
     * @throws NoSuchElementException   if there are no ports available
     */
    public static synchronized int findFreeLocalPort(int fromPort) {
        return findFreeLocalPort(fromPort, null);
    }

    /**
     * Gets the next available port starting at a given from port.
     *
     * @param fromPort    the from port to scan for availability
     * @param bindAddress the address that will try to bind
     * @return the available port
     * @throws IllegalArgumentException is thrown if the port number is out of range
     * @throws NoSuchElementException   if there are no ports available
     */
    public static synchronized int findFreeLocalPort(int fromPort, InetAddress bindAddress) {
        if (fromPort < currentMinPort.get() || fromPort > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("From port number not in valid range: " + fromPort);
        }

        for (int i = fromPort; i <= MAX_PORT_NUMBER; i++) {
            if (available(i, bindAddress)) {
                return i;
            }
        }

        throw new NoSuchElementException("Could not find an available port above " + fromPort);
    }

    /**
     * Gets the next available port starting at a given from port.
     *
     * @param bindAddresses the addresses that will try to bind
     * @return the available port
     * @throws IllegalArgumentException is thrown if the port number is out of range
     * @throws NoSuchElementException   if there are no ports available
     */
    public static synchronized int findFreeLocalPortOnAddresses(InetAddress... bindAddresses) {
        int fromPort = currentMinPort.get();
        if (fromPort < currentMinPort.get() || fromPort > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("From port number not in valid range: " + fromPort);
        }
        if (bindAddresses != null) {
            for (int j = fromPort; j <= MAX_PORT_NUMBER; j++) {
                for (InetAddress bindAddress : bindAddresses) {
                    if (available(j, bindAddress)) {
                        currentMinPort.set(j + 1);
                        return j;
                    }
                }
            }
        }

        throw new NoSuchElementException("Could not find an available port above " + fromPort);
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port number to check for availability
     * @return <tt>true</tt> if the port is available, or <tt>false</tt> if not
     * @throws IllegalArgumentException is thrown if the port number is out of range
     */
    public static boolean available(int port) throws IllegalArgumentException {
        return available(port, null);
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port        the port number to check for availability
     * @param bindAddress the address that will try to bind
     * @return <tt>true</tt> if the port is available, or <tt>false</tt> if not
     * @throws IllegalArgumentException is thrown if the port number is out of range
     */
    public static boolean available(int port, InetAddress bindAddress) throws IllegalArgumentException {
        if (port < currentMinPort.get() || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start currentMinPort: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = (bindAddress != null) ? new ServerSocket(port, 50, bindAddress) : new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = (bindAddress != null) ? new DatagramSocket(port, bindAddress) : new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            // Do nothing
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;

    }
}

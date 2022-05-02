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

import java.time.LocalDateTime;

/**
 * @author i1619kHz
 */
public class DefaultHttpResponse implements HttpResponse {
    public HttpResponse of(String content) {
        return null;
    }

    @Override
    public HttpResponse toJson(HttpStatus status, String content) {
        return null;
    }

    @Override
    public HttpResponse toJson(HttpHeader header, String content) {
        return null;
    }

    @Override
    public HttpResponse toJson(HttpStatus status, HttpHeader header, String content) {
        return null;
    }

    @Override
    public int status() {
        return 0;
    }

    @Override
    public String message() {
        return null;
    }

    @Override
    public String body() {
        return null;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public boolean headerSent() {
        return false;
    }

    @Override
    public void vary(CharSequence field) {

    }

    @Override
    public void redirect(CharSequence url, CharSequence alt) {

    }

    @Override
    public void attachment(CharSequence filename) {

    }

    @Override
    public String type() {
        return null;
    }

    @Override
    public LocalDateTime lastModified() {
        return null;
    }

    @Override
    public String etag() {
        return null;
    }

    @Override
    public void header(CharSequence key, CharSequence value) {

    }

    @Override
    public void remove(CharSequence key) {

    }

    @Override
    public boolean writable() {
        return false;
    }

    @Override
    public void flushHeaders() {

    }
}

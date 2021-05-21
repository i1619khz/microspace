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
package io.microspace.context.banner;

import io.leego.banana.BananaUtils;
import io.microspace.context.ansi.AnsiColor;
import io.microspace.context.ansi.AnsiOutput;

/**
 * @author i1619kHz
 */
public class DefaultApplicationBanner extends AbstractBanner {
    private final String MICRO_SPACE_VERSION = "(v1.0.0 RELEASE)";
    private final String MICRO_SPACE_FRAMEWORK = ":: microspace framework ::";

    @Override
    public void prePrintBannerText(String bannerText, String bannerFont) {
        System.out.println(BananaUtils.bananaify(bannerText, bannerFont));
    }

    @Override
    public String setUpPadding(Integer strapLineSize) {
        final StringBuilder padding = new StringBuilder();
        while (padding.length() < strapLineSize - (MICRO_SPACE_VERSION.length() + MICRO_SPACE_FRAMEWORK.length())) {
            padding.append(" ");
        }
        return padding.toString();
    }

    @Override
    public void printTextAndVersion(String padding) {
        System.out.println(AnsiOutput.toString(AnsiColor.GREEN, MICRO_SPACE_FRAMEWORK,
                AnsiColor.RESET, padding, MICRO_SPACE_VERSION));
        System.out.println();
    }
}

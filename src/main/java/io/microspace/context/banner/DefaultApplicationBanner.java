package io.microspace.context.banner;

import io.leego.banana.BananaUtils;
import io.microspace.context.ansi.AnsiColor;
import io.microspace.context.ansi.AnsiOutput;

import java.io.PrintStream;

/**
 * @author i1619kHz
 */
public class DefaultApplicationBanner extends AbstractBanner {
  private final String MICRO_SPACE_VERSION = "(v1.0.0 RELEASE)";
  private final String MICRO_SPACE_FRAMEWORK = ":: microspace framework ::";

  @Override
  public void prePrintBannerText(PrintStream printStream, String bannerText, String bannerFont) {
    printStream.println(BananaUtils.bananaify(bannerText, bannerFont));
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
  public void printTextAndVersion(PrintStream printStream, String padding) {
    printStream.println(AnsiOutput.toString(AnsiColor.GREEN, MICRO_SPACE_FRAMEWORK,
            AnsiColor.RESET, padding, MICRO_SPACE_VERSION));
    printStream.println();
  }
}

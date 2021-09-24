package octopus.teamcity.agent.logging;

import java.io.Serializable;
import java.util.Map;

import jetbrains.buildServer.agent.BuildProgressLogger;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(
    name = SdkLogAppender.SDK_APPENDER_NAME,
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class SdkLogAppender extends AbstractAppender {

  public static final String SDK_APPENDER_NAME = "SdkLogAppender";

  private final BuildProgressLogger buildProgressLogger;

  protected SdkLogAppender(
      String name,
      Filter filter,
      Layout<? extends Serializable> layout,
      boolean ignoreExceptions,
      Property[] properties,
      BuildProgressLogger logger) {
    super(name, filter, layout, ignoreExceptions, properties);
    this.buildProgressLogger = logger;
  }

  @PluginFactory
  public static SdkLogAppender createAppender(
      @PluginAttribute("Name") final String name,
      @PluginElement("BuildProcessLogger") final BuildProgressLogger buildProgressLogger) {
    return new SdkLogAppender(
        name,
        null,
        PatternLayout.createDefaultLayout(),
        true,
        new Property[] {},
        buildProgressLogger);
  }

  @Override
  public void append(final LogEvent event) {
    if (event != null
        && buildProgressLogger != null
        && event.getMessage().getFormattedMessage() != null) {
      buildProgressLogger.message(event.getMessage().getFormattedMessage());
    }
  }

  public static Level isVerboseLogging(final Map<String, String> runnerParameters) {
    if (runnerParameters != null
        && Boolean.parseBoolean(runnerParameters.get(CommonStepPropertyNames.VERBOSE_LOGGING))) {
      return Level.DEBUG;
    }
    return Level.INFO;
  }
}

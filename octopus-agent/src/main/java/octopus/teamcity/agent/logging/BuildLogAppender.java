package octopus.teamcity.agent.logging;

import java.io.Serializable;

import jetbrains.buildServer.agent.BuildProgressLogger;
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
    name = BuildLogAppender.BUILD_LOG_APPENDER_NAME,
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class BuildLogAppender extends AbstractAppender {

  public static final String BUILD_LOG_APPENDER_NAME = "BuildLogAppender";

  private final BuildProgressLogger buildProgressLogger;

  protected BuildLogAppender(
      final String name,
      final Filter filter,
      final Layout<? extends Serializable> layout,
      final boolean ignoreExceptions,
      final Property[] properties,
      final BuildProgressLogger logger) {
    super(name, filter, layout, ignoreExceptions, properties);
    this.buildProgressLogger = logger;
  }

  @PluginFactory
  public static AbstractAppender createAppender(
      @PluginAttribute("Name") final String name,
      @PluginElement("BuildProcessLogger") final BuildProgressLogger buildProgressLogger) {
    return new BuildLogAppender(
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
}

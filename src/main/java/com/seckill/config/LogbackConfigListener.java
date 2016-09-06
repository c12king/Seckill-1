package com.seckill.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @Author idler [idler41@163.com]
 * @Date 16/9/4 上午9:31.
 */
public class LogbackConfigListener implements ServletContextListener {

    private final Logger LOG = LoggerFactory.getLogger(LogbackConfigListener.class);

    private static final String CONFIG_LOCATION = "logback.xml";

    public void contextInitialized(ServletContextEvent event) {
        //从web.xml中加载指定文件名的日志配置文件
        String logbackConfigLocation = event.getServletContext().getInitParameter(CONFIG_LOCATION);
        try {

            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();

            JoranConfigurator joranConfigurator = new JoranConfigurator();
            joranConfigurator.setContext(loggerContext);

            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver =
                    new ServletContextResourcePatternResolver(event.getServletContext());
            Resource resource = pathMatchingResourcePatternResolver.getResource(logbackConfigLocation);
            joranConfigurator.doConfigure(resource.getFile());

            LOG.debug("loaded slf4j configure file from {}", resource.getFile().getAbsolutePath());
        } catch (Exception e) {
            LOG.error("can loading slf4j configure file from " + logbackConfigLocation, e);
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}

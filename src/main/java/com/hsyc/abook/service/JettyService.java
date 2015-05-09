package com.hsyc.abook.service;

import com.hsyc.abook.servlet.HelloServlet;
import com.hsyc.abook.servlet.SessionRequestServlet;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class JettyService {

  private static final Logger log = LoggerFactory.getLogger(JettyService.class);
  private Server server;
  public JettyService() {
    init();
  }

  private void init() {
    String jettyHome = System.getProperty("jetty.home", "./target");
    System.setProperty("jetty.home", jettyHome);

    // === jetty.xml ===
    // Setup Thread pool
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(500);

    // Server
    server = new Server(threadPool);
    // Scheduler
    server.addBean(new ScheduledExecutorScheduler());

    // HTTP Configuration
    HttpConfiguration http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
    http_config.setSecurePort(8043);
    http_config.setOutputBufferSize(32768);
    http_config.setRequestHeaderSize(8192);
    http_config.setResponseHeaderSize(8192);
    http_config.setSendServerVersion(true);
    http_config.setSendDateHeader(false);

    // Handler Structure
    HandlerCollection handlers = new HandlerCollection();
    ContextHandlerCollection contexts = new ContextHandlerCollection();

    // Servlets
    ServletContextHandler servlets = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servlets.setContextPath("/");
    servlets.setClassLoader(Thread.currentThread().getContextClassLoader());
    servlets.addServlet(new ServletHolder(new HelloServlet()), "/*");
//    servlets.addServlet(new ServletHolder(new ThoronServlet(thoronController)), "/track");

    // Web Applications
    File webAppDir = new File("webapp");
    if(!webAppDir.exists() || !webAppDir.isDirectory())
      log.error("Failed to Load Web Application : {}","webapp");
    String webAppDirPath = null;
    try {
      webAppDirPath = webAppDir.getCanonicalPath();
      WebAppContext webApp = new WebAppContext(webAppDirPath,"/knoa");
      webApp.setDefaultsDescriptor("webdefault.xml");
      webApp.setDescriptor(webAppDirPath+"/WEB-INF/web.xml");
      webApp.setResourceBase(webAppDirPath);
      webApp.setParentLoaderPriority(true);
      webApp.setWelcomeFiles(new String[] {webAppDirPath+"/index.html"});

      webApp.addServlet(new ServletHolder(new SessionRequestServlet()), "/requestTrack");
      contexts.addHandler(webApp);
    } catch (IOException e) {
      log.error("Failed to start web application");
    }
    contexts.addHandler(servlets);
    handlers.setHandlers(new Handler[]{contexts, new DefaultHandler()});

    server.setHandler(handlers);

    // Extra options
    server.setDumpAfterStart(false);
    server.setDumpBeforeStop(false);
    server.setStopAtShutdown(true);

    // === jetty-jmx.xml ===
    MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbContainer);

    // === jetty-http.xml ===
    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
    http.setPort(8080);
    http.setIdleTimeout(60000);
    server.addConnector(http);

    // === jetty-requestlog.xml ===
    NCSARequestLog requestLog = new NCSARequestLog();
    requestLog.setFilename(jettyHome + "/logs/yyyy_mm_dd.request.log");
    requestLog.setFilenameDateFormat("yyyy_MM_dd");
    requestLog.setRetainDays(90);
    requestLog.setAppend(true);
    requestLog.setExtended(true);
    requestLog.setLogCookies(false);
    requestLog.setLogTimeZone("EST");
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    requestLogHandler.setRequestLog(requestLog);
    handlers.addHandler(requestLogHandler);

    // === jetty-lowresources.xml ===
    LowResourceMonitor lowResourcesMonitor = new LowResourceMonitor(server);
    lowResourcesMonitor.setPeriod(1000);
    lowResourcesMonitor.setLowResourcesIdleTimeout(200);
    lowResourcesMonitor.setMonitorThreads(true);
    lowResourcesMonitor.setMaxConnections(0);
    lowResourcesMonitor.setMaxMemory(0);
    lowResourcesMonitor.setMaxLowResourcesTime(5000);
    server.addBean(lowResourcesMonitor);
  }

  public void start() throws Exception {
    log.info("Jetty service is starting...");
    server.start();
    log.info("Jetty service is started");
  }

  public void stop() throws Exception {
    log.info("Jetty Service is stopping...");
    server.join();
    server.stop();
    log.info("Jetty Service is stopped");
  }

}

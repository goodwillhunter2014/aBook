package com.hsyc.abook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpService {

  private static final Logger log = LoggerFactory.getLogger(HttpService.class);
  private JettyService jetty;

  public HttpService() {
    jetty = new JettyService();
  }

  public void start() throws Exception {
    log.info("Http services starting...");
    jetty.start();

    log.info("Http services started successfully");
  }

  public void stop() throws Exception {
    log.info("Http services stopping...");
    jetty.stop();
    log.info("Http services stopped successfully");
  }
}

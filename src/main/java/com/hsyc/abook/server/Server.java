package com.hsyc.abook.server;

import com.hsyc.abook.service.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

  private static final Logger log = LoggerFactory.getLogger(Server.class);
  private HttpService httpService;
  private boolean started;
  public Server() {
    httpService = new HttpService();
    started = false;
  }

  private void start() throws Exception {
    log.info("Sever Starting...");
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        synchronized (Server.this) {
          log.info("check...");
          if(started) {
            try {
              Server.this.stop();
            } catch (Exception e) {
              log.error("Error {}", e);
            }
          } else {
            mainThread.interrupt();
          }
        }
      }
    });
    httpService.start();
    started = true;
    log.info("Server started successfully");
  }

  private void stop() throws Exception {
    log.info("Server Stopping...");
    httpService.stop();
    log.info("Server stopped successfully - bye");
  }

  public static void main(String[] args) {
    log.info("Hyesun and Yongchul Account Book Server");
    try {
      new Server().start();
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

}

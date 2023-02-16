package com.chilleric.franchise_sys.log;

public enum LoggerType {
  REQUEST("requestLog"),
  APPLICATION("applicationLog"),
  API("apiLog"),
  SQL("sqlLog");

  private String loggerName;

  LoggerType(String loggerName) {
    this.loggerName = loggerName;
  }

  public String getLoggerName() {
    return loggerName;
  }
}

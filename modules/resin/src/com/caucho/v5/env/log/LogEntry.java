/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.log;

/**
 * Persistent logging.
 */
@SuppressWarnings("serial")
public class LogEntry implements java.io.Serializable
{
  private long _timestamp;
  private String _message;

  LogEntry()
  {
  }

  public void setTimestamp(long timestamp)
  {
    _timestamp = timestamp;
  }

  public long getTimestamp()
  {
    return _timestamp;
  }

  public void setMessage(String message)
  {
    _message = message;
  }

  public String getMessage()
  {
    return _message;
  }
}

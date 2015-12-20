/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.util.CurrentTime;

public class HealthActionResult
{
  public static enum ResultStatus
  {
    OK,
    SKIPPED,
    FAILED
  }
  
  private ResultStatus _status;
  private String _message;
  private long _timestamp = CurrentTime.getCurrentTime();
  private Throwable _exception;

  public HealthActionResult()
  {
    _status = ResultStatus.OK;
  }

  public HealthActionResult(ResultStatus status)
  {
    this(status, status.toString());
  }

  public HealthActionResult(ResultStatus status, String message)
  {
    _status = status;
    _message = message;
  }
  
  public ResultStatus getStatus()
  {
    return _status;
  }

  public void setStatus(ResultStatus status)
  {
    _status = status;
  }

  public String getMessage()
  {
    return _message;
  }

  public void setMessage(String message)
  {
    _message = message;
  }
  
  public long getTimestamp()
  {
    return _timestamp;
  }

  public void setTimestamp(long timestamp)
  {
    _timestamp = timestamp;
  }
  
  public Throwable getException()
  {
    return _exception;
  }

  public void setException(Throwable exception)
  {
    _exception = exception;
  }
  
  public boolean isSuccess()
  {
    return _status == ResultStatus.OK;
  }
  
  public boolean isFailure()
  {
    return _status == ResultStatus.FAILED;
  }
  
  public boolean isSkipped()
  {
    return _status == ResultStatus.SKIPPED;
  }
  
  public String getDescription()
  {
    return _status + ": " + _message;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getDescription() + "]";
  }
}

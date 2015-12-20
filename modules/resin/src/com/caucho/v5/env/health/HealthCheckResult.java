/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;

public class HealthCheckResult
{
  private HealthStatus _status;
  private String _message;
  private long _timestamp = CurrentTime.getCurrentTime();
  private boolean _recover = false;
  private Throwable _exception;

  public HealthCheckResult()
  {
    
  }

  public HealthCheckResult(HealthStatus status)
  {
    this(status, status.toString());
  }

  public HealthCheckResult(HealthStatus status, String message)
  {
    _status = status;
    _message = message;
  }
  
  public HealthStatus getStatus()
  {
    return _status;
  }

  public void setStatus(HealthStatus status)
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
  
  public boolean isRecover()
  {
    return _recover;
  }

  public void setRecover(boolean recover)
  {
    _recover = recover;
  }

  public Throwable getException()
  {
    return _exception;
  }

  public void setException(Throwable exception)
  {
    _exception = exception;
  }
  
  public boolean isOk()
  {
    return _status == HealthStatus.OK;
  }
  
  public boolean isWarning()
  {
    return _status == HealthStatus.WARNING;
  }
  
  public boolean isCritical()
  {
    return _status == HealthStatus.CRITICAL;
  }
  
  public boolean isFatal()
  {
    return _status == HealthStatus.FATAL;
  }
  
  public boolean isUnknown()
  {
    return _status == HealthStatus.UNKNOWN;
  }
  
  public String getDescription()
  {
    return _status + ": " + _message;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _status + ":" + _message + "]";
  }
}

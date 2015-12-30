/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.event;

import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.shutdown.ExitCode;

public class RestartHealthEvent extends HealthEvent
{
  public static final String EVENT_NAME = "caucho.health.restart";
  
  private String _startMessage;
  private ExitCode _exitCode;
  
  public RestartHealthEvent(HealthSubSystem healthSystem,
                            String _startMessage,
                            ExitCode _exitCode)
  {
    super(healthSystem, EVENT_NAME);
  }

  public String getStartMessage()
  {
    return _startMessage;
  }

  public void setStartMessage(String startMessage)
  {
    _startMessage = startMessage;
  }

  public ExitCode getExitCode()
  {
    return _exitCode;
  }

  public void setExitCode(ExitCode exitCode)
  {
    _exitCode = exitCode;
  }
}

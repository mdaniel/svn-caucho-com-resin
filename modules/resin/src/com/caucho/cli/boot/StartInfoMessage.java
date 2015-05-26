/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.cli.boot;

import java.io.Serializable;

import com.caucho.env.shutdown.ExitCode;

/**
 * Queries the Resin instance for its pid.
 */
@SuppressWarnings("serial")
public class StartInfoMessage implements Serializable {
  private boolean _isRestart;
  private String _restartMessage;
  private String _shutdownMessage;
  private ExitCode _previousExitCode;
  
  public StartInfoMessage()
  {
  }
  
  public StartInfoMessage(boolean isRestart, 
                          String message,
                          ExitCode previousExitCode,
                          String shutdownMessage)
  {
    _isRestart = isRestart;
    _restartMessage = message;
    _previousExitCode = previousExitCode;
    _shutdownMessage = shutdownMessage;
  }
  
  /**
   * Returns true if Resin was restarted.
   */
  public boolean isRestart()
  {
    return _isRestart;
  }
  
  /**
   * Returns the Resin start message.
   */
  public String getRestartMessage()
  {
    return _restartMessage;
  }
  
  public ExitCode getPreviousExitCode()
  {
    return _previousExitCode;
  }

  @Override
  public String toString()
  {
    return String.format("%s[%s,%s]", getClass().getSimpleName(), _isRestart, _restartMessage);
  }
}

/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package javax.mail;

import java.util.Properties;

import java.io.PrintStream;

/**
 * Represents a mail session.
 */
public abstract class Session {
  private static Session _defaultSession;

  private boolean _isDebug;
  private PrintStream _debugOut;
  
  /**
   * Returns a new session object.
   */
  public static Session getInstance(Properties props,
				    Authenticator authenticator)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a new session object.
   */
  public static Session getInstance(Properties props)
  {
    return getInstance(props, null);
  }
  
  /**
   * Returns the default session object.
   */
  public static Session getDefaultInstance(Properties props,
					   Authenticator authenticator)
  {
    if (_defaultSession == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

	_defaultSession = getInstance(props, authenticator);
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }

    return _defaultSession;
  }
  
  /**
   * Sets the debug value.
   */
  public void setDebug(boolean debug)
  {
    _isDebug = debug;
  }
  
  /**
   * Gets the debug value.
   */
  public boolean getDebug()
  {
    return _isDebug;
  }
  
  /**
   * Sets the debug output stream;
   */
  public void setDebugOut(PrintStream out)
  {
    _debugOut = out;
  }
  
  /**
   * Gets the debug output stream;
   */
  public PrintStream getDebugOut()
  {
    return _debugOut;
  }
}

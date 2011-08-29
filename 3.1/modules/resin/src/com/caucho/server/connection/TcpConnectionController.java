/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.connection;

import java.util.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.servlet.comet.CometController;

import com.caucho.server.port.*;
import com.caucho.server.connection.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Public API to control a http upgrade connection.
 */
public class TcpConnectionController extends ConnectionController
{
  private static final L10N L = new L10N(TcpConnectionController.class);
  private static final Logger log
    = Logger.getLogger(TcpConnectionController.class.getName());

  private ClassLoader _loader;
  
  private AbstractHttpRequest _request;
  private long _maxIdleTime;

  private ReadStream _is;
  private WriteStream _os;

  private TcpConnectionHandler _handler;

  private ReadTask _readTask = new ReadTask();
  private WriteTask _writeTask = new WriteTask();

  public TcpConnectionController(ServletRequest request,
				 TcpConnectionHandler handler)
  {
    this(getAbstractHttpRequest(request), handler);
  }

  public TcpConnectionController(AbstractHttpRequest request,
				 TcpConnectionHandler handler)
  {
    super(request.getConnection());

    if (handler == null)
      throw new NullPointerException(L.l("handler is a required argument"));

    _handler = handler;
    
    _request = request;
    _loader = Thread.currentThread().getContextClassLoader();

    _is = getConnection().getReadStream();
    _os = getConnection().getWriteStream();
  }

  private static AbstractHttpRequest
    getAbstractHttpRequest(ServletRequest request)
  {
    return (AbstractHttpRequest) request;
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return true;
  }
  
  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      _maxIdleTime = Long.MAX_VALUE / 2;
  }
  
  /**
   * Gets the max idle time.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  /**
   * Returns the read task
   */
  public Runnable getReadTask()
  {
    return _readTask;
  }

  /**
   * Returns the write task
   */
  public Runnable getWriteTask()
  {
    return _writeTask;
  }

  /**
   * Returns the handler
   */
  public TcpConnectionHandler getHandler()
  {
    return _handler;
  }

  /**
   * Closes the connection.
   */
  @Override
  public void close()
  {
    _request = null;
    _is = null;
    _os = null;
    _readTask = null;
    _writeTask = null;
    _loader = null;
    
    super.close();
  }

  public String toString()
  {
    AbstractHttpRequest request = _request;

    if (request == null || request.getConnection() == null)
      return "TcpConnectionController[closed]";
    else if (Alarm.isTest())
      return "TcpConnectionController[" + _handler + "]";
    else
      return ("TcpConectionController["
	      + request.getConnection().getId()
	      + "," + _handler + "]");
  }

  class ReadTask implements Runnable {
    public void run()
    {
      Thread thread = Thread.currentThread();
      
      boolean isValid = false;

      String oldName = thread.getName();
      
      try {
	thread.setName("resin-" + _handler.getClass().getSimpleName()
		       + "-read-" + getConnection().getId());
	thread.setContextClassLoader(_loader);

	while (isActive()) {
	  TcpConnection conn = (TcpConnection) getConnection();
	  ReadStream is = _is;

	  if (conn == null || is == null)
	    return;
	  else if (! conn.waitForKeepalive() && conn.waitForSelect()) {
	    isValid = true;
	    return;
	  }
	  else if (! _handler.serviceRead(is, TcpConnectionController.this)) {
	    return;
	  }
	}
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      } finally {
	thread.setName(oldName);
	if (! isValid)
	  close();
      }
    }
  }

  class WriteTask implements Runnable {
    public void run()
    {
      Thread thread = Thread.currentThread();
      
      boolean isValid = false;

      thread.setName("resin-" + _handler.getClass().getSimpleName()
		     + "-write-" + getConnection().getId());
      
      try {
	thread.setContextClassLoader(_loader);
	
	while (isActive()) {
	  WriteStream os = _os;

	  if (! _handler.serviceWrite(os, TcpConnectionController.this))
	    return;
	  else if (((TcpConnection) getConnection()).suspendWrite()) {
	    isValid = true;
	    return;
	  }
	}
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      } finally {
	if (! isValid)
	  close();
      }
    }
  }
}

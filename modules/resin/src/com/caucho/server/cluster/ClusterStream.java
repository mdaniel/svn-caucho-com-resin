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

package com.caucho.server.cluster;

import com.caucho.hessian.io.*;
import com.caucho.server.hmux.*;
import com.caucho.util.Alarm;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.util.logging.*;

/**
 * Defines a connection to the client.
 */
public class ClusterStream {
  static protected final Logger log
    = Logger.getLogger(ClusterStream.class.getName());

  private ServerConnector _srun;

  private ReadStream _is;
  private WriteStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private long _freeTime;

  private String _debugId;

  ClusterStream(int count, ServerConnector client,
		ReadStream is, WriteStream os)
  {
    _srun = client;
    _is = is;
    _os = os;

    _debugId = "[" + client.getDebugId() + ":" + count + "]";
  }

  /**
   * Returns the cluster server.
   */
  public ServerConnector getServer()
  {
    return _srun;
  }

  /**
   * Returns the input stream.
   */
  public ReadStream getReadStream()
  {
    _freeTime = 0;
    
    return _is;
  }

  /**
   * Returns the write stream.
   */
  public WriteStream getWriteStream()
  {
    _freeTime = 0;
    
    return _os;
  }

  /**
   * Returns the hessian input stream
   */
  public Hessian2StreamingInput getHessianInputStream()
  {
    if (_in == null)
      _in = new Hessian2StreamingInput(_is);

    return _in;
  }

  /**
   * Returns the hessian output stream
   */
  public Hessian2StreamingOutput getHessianOutputStream()
  {
    if (_out == null)
      _out = new Hessian2StreamingOutput(_os);

    return _out;
  }

  /**
   * Returns the free time, i.e. the time the connection was last idle.
   */
  public long getFreeTime()
  {
    return _freeTime;
  }

  /**
   * Sets the free time.
   */
  public void setFreeTime(long freeTime)
  {
    _freeTime = freeTime;
  }

  /**
   * Returns true if nearing end of free time.
   */
  public boolean isLongIdle()
  {
    return (_srun.getLoadBalanceIdleTime()
	    < Alarm.getCurrentTime() - _freeTime + 2000L);
  }

  public boolean sendMessage(String to, String from,
			     Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_MESSAGE);
    out.write(0);
    out.write(0);

    writeString(out, to);
    writeString(out, from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean sendQueryGet(long id, String to, String from,
			      Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_GET);
    out.write(0);
    out.write(8);

    writeLong(out, id);
    writeString(out, to);
    writeString(out, from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean sendQuerySet(long id, String to, String from,
			      Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_SET);
    out.write(0);
    out.write(8);

    writeLong(out, id);
    writeString(out, to);
    writeString(out, from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean sendQueryResult(long id, String to, String from,
				 Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_RESULT);
    out.write(0);
    out.write(8);

    writeLong(out, id);
    writeString(out, to);
    writeString(out, from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    return _debugId;
  }

  /**
   * Clears the recycled connections.
   */
  public void clearRecycle()
  {
    _srun.clearRecycle();
  }

  /**
   * Recycles.
   */
  public void free()
  {
    // #2369 - the load balancer might set its own view of the free
    // time
    if (_is != null && _freeTime <= 0)
      _freeTime = _is.getReadTime();

    _srun.free(this);
  }

  public void close()
  {
    if (_is != null)
      _srun.close(this);

    closeImpl();
  }
  
  /**
   * closes the stream.
   */
  void closeImpl()
  {
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;
    
    try {
      if (is != null)
	is.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      if (os != null)
	os.close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  private void writeLong(WriteStream out, long id)
    throws IOException
  {
    out.write((int) (id >> 56));
    out.write((int) (id >> 48));
    out.write((int) (id >> 40));
    out.write((int) (id >> 32));
    out.write((int) (id >> 24));
    out.write((int) (id >> 16));
    out.write((int) (id >> 8));
    out.write((int) id);
  }

  private void writeString(WriteStream out, String s)
    throws IOException
  {
    int len = s.length();
    
    out.write(HmuxRequest.HMUX_STRING);
    out.write(len >> 8);
    out.write(len);
    out.print(s);
  }

  public String toString()
  {
    return "ClusterStream[" + _debugId + "]";
  }
}

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

  private ServerPool _pool;

  private ReadStream _is;
  private WriteStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  private long _freeTime;

  private String _debugId;

  ClusterStream(ServerPool pool, int count, 
		ReadStream is, WriteStream os)
  {
    _pool = pool;
    _is = is;
    _os = os;

    _debugId = "[" + pool.getDebugId() + ":" + count + "]";
  }

  /**
   * Returns the owning pool
   */
  public ServerPool getPool()
  {
    return _pool;
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
    long now = Alarm.getCurrentTime();
    
    return (_pool.getLoadBalanceIdleTime() < now - _freeTime + 2000L);
  }

  public boolean message(String to, String from,
			 Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_MESSAGE);
    out.write(0);
    out.write(0);

    writeString(to);
    writeString(from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean queryGet(long id, String to, String from,
			      Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_GET);
    out.write(0);
    out.write(8);

    writeLong(id);
    writeString(to);
    writeString(from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean querySet(long id, String to, String from,
			      Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_SET);
    out.write(0);
    out.write(8);

    writeLong(id);
    writeString(to);
    writeString(from);

    Hessian2StreamingOutput hOut = getHessianOutputStream();

    hOut.writeObject(query);

    out.write(HmuxRequest.HMUX_QUIT);
    out.flush();

    return true;
  }

  public boolean queryResult(long id, String to, String from,
				 Serializable query)
    throws IOException
  {
    WriteStream out = getWriteStream();

    out.write(HmuxRequest.HMTP_QUERY_RESULT);
    out.write(0);
    out.write(8);

    writeLong(id);
    writeString(to);
    writeString(from);

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
    _pool.clearRecycle();
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

    _pool.free(this);
  }

  public void close()
  {
    if (_is != null)
      _pool.close(this);

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

  /**
   * Writes a hmux exit to the target.
   */
  public void writeExit()
    throws IOException
  {
    _os.write(HmuxRequest.HMUX_EXIT);
    _os.flush();
  }

  /**
   * Writes a hmux quit to the target.
   */
  public void writeQuit()
    throws IOException
  {
    _os.write(HmuxRequest.HMUX_QUIT);
    _os.flush();
  }

  /**
   * Writes a hmux yield to the target, used for unidirectional messages
   */
  public void writeYield()
    throws IOException
  {
    _os.write(HmuxRequest.HMUX_YIELD);
    _os.flush();
  }

  /**
   * Writes a hmux int to the target.
   */
  public void writeInt(int code, int value)
    throws IOException
  {
    WriteStream os = _os;
    
    os.write(code);
    os.write(0);
    os.write(4);
    os.write(value >> 24);
    os.write(value >> 16);
    os.write(value >> 8);
    os.write(value);
  }

  public void writeLong(long id)
    throws IOException
  {
    WriteStream os = _os;
    
    os.write((int) (id >> 56));
    os.write((int) (id >> 48));
    os.write((int) (id >> 40));
    os.write((int) (id >> 32));
    os.write((int) (id >> 24));
    os.write((int) (id >> 16));
    os.write((int) (id >> 8));
    os.write((int) id);
  }

  public void writeLong(int code, long id)
    throws IOException
  {
    WriteStream os = _os;
    
    os.write(code);
    os.write(0);
    os.write(8);
    os.write((int) (id >> 56));
    os.write((int) (id >> 48));
    os.write((int) (id >> 40));
    os.write((int) (id >> 32));
    os.write((int) (id >> 24));
    os.write((int) (id >> 16));
    os.write((int) (id >> 8));
    os.write((int) id);
  }

  public void writeString(String s)
    throws IOException
  {
    int len = s.length();

    WriteStream os = _os;
    
    os.write(HmuxRequest.HMUX_STRING);
    os.write(len >> 8);
    os.write(len);
    os.print(s);
  }

  public void writeString(int code, String s)
    throws IOException
  {
    int len = s.length();

    WriteStream os = _os;

    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(s);
  }

  /**
   * Writes a hmux string to the target.
   */
  public void writeBinary(int code, byte []value)
    throws IOException
  {
    int len = value.length;

    WriteStream os = _os;
    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.write(value, 0, len);
  }

  /**
   * Writes a hmux string to the target.
   */
  public void writeBinary(int code, byte []value, int offset, int len)
    throws IOException
  {
    WriteStream os = _os;
    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.write(value, offset, len);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _debugId + "]";
  }
}

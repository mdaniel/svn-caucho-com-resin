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

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorException;
import com.caucho.bam.ActorStream;
import com.caucho.hessian.io.*;
import com.caucho.server.hmux.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.*;

/**
 * Defines a connection to the client.
 */
public class ClusterStream implements ActorStream {
  private static final L10N L = new L10N(ClusterStream.class);
  
  private static final Logger log
    = Logger.getLogger(ClusterStream.class.getName());

  private ServerPool _pool;

  private ReadStream _is;
  private WriteStream _os;

  private Hessian2StreamingInput _in;
  private Hessian2Output _out;

  private boolean _isAuthenticated;

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
   * Returns true if the stream has been authenticated
   */
  public boolean isAuthenticated()
  {
    return _isAuthenticated;
  }

  /**
   * Returns true if the stream has been authenticated
   */
  public void setAuthenticated(boolean isAuthenticated)
  {
    _isAuthenticated = isAuthenticated;
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
  public Hessian2Output getHessianOutputStream()
  {
    if (_out == null)
      _out = new Hessian2Output(_os);

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
   * Sets the free time.
   */
  public void clearFreeTime()
  {
    _freeTime = 0;
    _is.clearReadTime();
  }

  /**
   * Returns true if nearing end of free time.
   */
  public boolean isLongIdle()
  {
    long now = Alarm.getCurrentTime();
    
    return (_pool.getLoadBalanceIdleTime() < now - _freeTime + 2000L);
  }

  //
  // ActorStream output for HMTP
  //

  public String getJid()
  {
    return "clusterStream@admin.resin";
  }

  public void message(String to, String from, Serializable query)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_MESSAGE);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();
      hOut.writeString(to);
      hOut.writeString(from);

      hOut.writeObject(query);
      hOut.endPacket();
      hOut.flushBuffer();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void messageError(String to,
			   String from,
			   Serializable query,
			   ActorError error)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_MESSAGE_ERROR);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();
      hOut.writeString(to);
      hOut.writeString(from);

      hOut.writeObject(query);
      hOut.writeObject(error);
      
      hOut.endPacket();
      hOut.flushBuffer();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void queryGet(long id, String to, String from,
			  Serializable query)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_QUERY_GET);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();

      hOut.writeString(to);
      hOut.writeString(from);
      hOut.writeLong(id);
      hOut.writeObject(query);
      hOut.endPacket();
      hOut.flushBuffer();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void querySet(long id,
		       String to,
		       String from,
		       Serializable query)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_QUERY_SET);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();
      hOut.writeString(to);
      hOut.writeString(from);
      hOut.writeLong(id);

      hOut.writeObject(query);
      hOut.endPacket();
      hOut.flushBuffer();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_QUERY_RESULT);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();
      hOut.writeString(to);
      hOut.writeString(from);
      hOut.writeLong(id);
      hOut.writeObject(query);
      hOut.endPacket();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 ActorError error)
  {
    try {
      WriteStream out = getWriteStream();

      out.write(HmuxRequest.HMTP_QUERY_ERROR);
      out.write(0);
      out.write(1);
      boolean isAdmin = true;
      out.write(isAdmin ? 1 : 0);

      Hessian2Output hOut = getHessianOutputStream();

      hOut.startPacket();
      hOut.writeString(to);
      hOut.writeString(from);
      hOut.writeLong(id);
    
      hOut.writeObject(query);
      hOut.writeObject(error);
      hOut.endPacket();
    } catch (IOException e) {
      throw new ActorException(e);
    }
  }

  public void presence(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceUnavailable(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceProbe(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceSubscribe(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceSubscribed(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceUnsubscribe(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceUnsubscribed(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void presenceError(String to,
			    String from,
			    Serializable value,
			    ActorError error)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // HMTP readers
  //
  
  public Serializable readQueryResult(long id)
    throws IOException
  {
    ReadStream in = getReadStream();

    int code = in.read();

    if (code < 0)
      throw new EOFException(L.l("unexpected end of file"));
    else if (code == HmuxRequest.HMTP_QUERY_RESULT)
      return parseQueryResult(id, in);
    else if (code == HmuxRequest.HMTP_QUERY_ERROR)
      return parseQueryError(id, in);
    else
      throw new IOException(L.l("expected query result at '" +
				(char) code + "' " + code));
  }

  public Serializable parseQueryResult(long id, ReadStream is)
    throws IOException
  {
    int len = (is.read() << 8) + is.read();
    boolean isAdmin = is.read() != 0;

    Hessian2StreamingInput hInStream = getHessianInputStream();

    Hessian2Input hIn = hInStream.startPacket();

    String to = hIn.readString();
    String from = hIn.readString();

    long resultId = hIn.readLong();

    Serializable result = (Serializable) hIn.readObject();

    hInStream.endPacket();

    return result;
  }

  public Serializable parseQueryError(long id, ReadStream is)
    throws IOException
  {
    int len = (is.read() << 8) + is.read();
    boolean isAdmin = is.read() != 0;

    Hessian2StreamingInput hInStream = getHessianInputStream();

    Hessian2Input hIn = hInStream.startPacket();

    String to = hIn.readString();
    String from = hIn.readString();

    long resultId = hIn.readLong();

    Serializable result = (Serializable) hIn.readObject();
    ActorError error = (ActorError) hIn.readObject();

    hInStream.endPacket();

    throw error.createException();
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
    if (_is != null && _freeTime <= 0) {
      _freeTime = _is.getReadTime();
      
      if (_freeTime <= 0) {
	// for write-only, the read time is zero
	_freeTime = Alarm.getCurrentTime();
      }
    }

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
   * Flush to the target
   */
  public void flush()
    throws IOException
  {
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
    WriteStream os = _os;
    
    if (value == null) {
      os.write(code);
      os.write(0);
      os.write(0);
      return;
    }
    
    int len = value.length;

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

  public long readLongValue()
    throws IOException
  {
    ReadStream is = _is;

    return (((long) is.read() << 56)
	    + ((long) is.read() << 48)
	    + ((long) is.read() << 40)
	    + ((long) is.read() << 32)
	    + ((long) is.read() << 24)
	    + ((long) is.read() << 16)
	    + ((long) is.read() << 8)
	    + ((long) is.read() << 0));
  }

  public String readStringValue()
    throws IOException
  {
    ReadStream is = _is;
    
    int len = (is.read() << 8) + is.read();

    char []data = new char[len];

    for (int i = 0; i < len; i++)
      data[i] = (char) is.read();

    return new String(data);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _debugId + "]";
  }
}

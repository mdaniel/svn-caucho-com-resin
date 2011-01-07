/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.ActorError;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugOutputStream;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
public class HmtpWriter
{
  private static final Logger log
    = Logger.getLogger(HmtpWriter.class.getName());

  private String _jid;
  
  private Hessian2Output _out;
  private HessianDebugOutputStream _dOut;
  
  private int _jidCacheIndex;
  private HashMap<String,Integer> _jidCache = new HashMap<String,Integer>(256);
  private String []_jidCacheRing = new String[256];

  public HmtpWriter()
  {
    _out = new Hessian2Output();
    
    if (log.isLoggable(Level.FINEST)) {
      _dOut = new HessianDebugOutputStream(log, Level.FINEST);
    }
  }

  protected void init(OutputStream os)
  {
    if (_dOut != null) {
      _dOut.initPacket(os);
      os = _dOut;
    }
    
    _out.initPacket(os);
  }
  
  /**
   * The jid of the stream
   */
  public String getJid()
  {
    return _jid;
  }
  
  /**
   * The jid of the stream
   */
  public void setJid(String jid)
  {
    _jid = jid;
  }

  //
  // message
  //

  /**
   * Sends a message to a given jid
   * 
   * @param os the output stream for the message
   * @param to the jid of the target actor
   * @param from the jid of the source actor
   * @param payload the message payload
   */
  public void message(OutputStream os, 
                      String to, 
                      String from, 
                      Serializable payload)
    throws IOException
  {
    init(os);

    Hessian2Output out = _out;

    if (out == null)
      return;

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " message " + payload
                + " {to:" + to + ", from:" + from + "}");
    }

    out.writeInt(HmtpPacketType.MESSAGE.ordinal());
    writeJid(out, to);
    writeJid(out, from);
    out.writeObject(payload);

    out.flushBuffer();
  }

  /**
   * Sends a message error to a given jid
   */
  public void messageError(OutputStream os,
                           String to,
                           String from,
                           Serializable value,
                           ActorError error)
    throws IOException
  {
    init(os);
    
    Hessian2Output out = _out;
    
    if (out == null)
      return;

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " messageError " + value
                 + " {to:" + to + ", from:" + from + "}");
    }

    out.writeInt(HmtpPacketType.MESSAGE_ERROR.ordinal());
    writeJid(out, to);
    writeJid(out, from);
    out.writeObject(value);
    out.writeObject(error);
    
    out.flushBuffer();
  }

  //
  // query
  //

  /**
   * Low-level query
   */
  public void query(OutputStream os,
                    long id,
                    String to,
                    String from,
                    Serializable value)
    throws IOException
  {
    init(os);
    
    Hessian2Output out = _out;

    if (out == null)
      return;

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " query " + value
                + " {id: " + id + ", to:" + to + ", from:" + from + "}");
    }

    out.writeInt(HmtpPacketType.QUERY.ordinal());
    writeJid(out, to);
    writeJid(out, from);
    out.writeLong(id);
    out.writeObject(value);

    out.flushBuffer();
  }

  /**
   * Low-level query response
   */
  public void queryResult(OutputStream os,
                          long id,
                          String to,
                          String from,
                          Serializable value)
    throws IOException
  {
    init(os);
    
    Hessian2Output out = _out;

    if (out == null)
      return;

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " queryResult " + value
                + " {id: " + id + ", to:" + to + ", from:" + from + "}");
    }

    out.writeInt(HmtpPacketType.QUERY_RESULT.ordinal());
    writeJid(out, to);
    writeJid(out, from);
    out.writeLong(id);
    out.writeObject(value);
    
    out.flushBuffer();
  }

  /**
   * Low-level query error
   */
  public void queryError(OutputStream os,
                         long id,
                         String to,
                         String from,
                         Serializable value,
                         ActorError error)
    throws IOException
  {
    init(os);

    Hessian2Output out = _out;

    if (out == null)
      return;

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " queryError " + error + " " + value
                + " {id: " + id + ", to:" + to + ", from:" + from + "}");
    }

    out.writeInt(HmtpPacketType.QUERY_ERROR.ordinal());
    writeJid(out, to);
    writeJid(out, from);
    out.writeLong(id);
    out.writeObject(value);
    out.writeObject(error);
    
    out.flushBuffer();
  }
  
  private void writeJid(Hessian2Output out, String jid)
    throws IOException
  {
    if (jid == null) {
      out.writeString(null);
      return;
    }
    
    Integer value = _jidCache.get(jid);
    
    if (value != null)
      out.writeInt(value);
    else {
      out.writeString(jid);
      
      int index = _jidCacheIndex;
      
      _jidCacheIndex = (index + 1) % _jidCacheRing.length;
      
      if (_jidCacheRing[index] != null) {
        _jidCache.remove(_jidCacheRing[index]);
      }
      
      _jidCacheRing[index] = jid;
      _jidCache.put(jid, index);
    }
  }

  public void flush()
    throws IOException
  {
    Hessian2Output out = _out;

    if (out != null) {
      out.flush();
    }
  }

  public void close()
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " close");

    try {
      Hessian2Output out = _out;
      _out = null;

      if (out != null) {
        out.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}

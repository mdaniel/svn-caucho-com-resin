/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.bam.BamError;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugOutputStream;

/**
 * HmtpWriteStream writes HMTP packets to an OutputStream.
 */
public class HmtpWriter
{
  private static final Logger log
    = Logger.getLogger(HmtpWriter.class.getName());

  private String _address;
  
  private Hessian2Output _out;
  private HessianDebugOutputStream _dOut;
  
  private int _addressCacheIndex;
  private HashMap<String,Integer> _addressCache = new HashMap<String,Integer>(256);
  private String []_addressCacheRing = new String[256];

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
   * The address of the stream
   */
  public String getAddress()
  {
    return _address;
  }
  
  /**
   * The address of the stream
   */
  public void setAddress(String address)
  {
    _address = address;
  }

  //
  // message
  //

  /**
   * Sends a message to a given address
   * 
   * @param os the output stream for the message
   * @param to the address of the target actor
   * @param from the address of the source actor
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
    writeAddress(out, to);
    writeAddress(out, from);
    out.writeObject(payload);

    out.flushBuffer();
  }

  /**
   * Sends a message error to a given address
   */
  public void messageError(OutputStream os,
                           String to,
                           String from,
                           Serializable value,
                           BamError error)
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
    writeAddress(out, to);
    writeAddress(out, from);
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
    writeAddress(out, to);
    writeAddress(out, from);
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
    writeAddress(out, to);
    writeAddress(out, from);
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
                         BamError error)
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
    writeAddress(out, to);
    writeAddress(out, from);
    out.writeLong(id);
    out.writeObject(value);
    out.writeObject(error);
    
    out.flushBuffer();
  }
  
  private void writeAddress(Hessian2Output out, String address)
    throws IOException
  {
    if (address == null) {
      out.writeString(null);
      return;
    }
    
    Integer value = _addressCache.get(address);
    
    if (value != null)
      out.writeInt(value);
    else {
      out.writeString(address);
      
      int index = _addressCacheIndex;
      
      _addressCacheIndex = (index + 1) % _addressCacheRing.length;
      
      if (_addressCacheRing[index] != null) {
        _addressCache.remove(_addressCacheRing[index]);
      }
      
      _addressCacheRing[index] = address;
      _addressCache.put(address, index);
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
    return getClass().getSimpleName() + "[" + getAddress() + "]";
  }
}

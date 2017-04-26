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
import java.io.InputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.stream.MessageStream;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2StreamingInput;

/**
 * HmtpReader stream handles client packets received from the server.
 */
public class HmtpReader {
  private static final Logger log
    = Logger.getLogger(HmtpReader.class.getName());

  private String _id;
  
  private InputStream _is;
  private Hessian2StreamingInput _in;
  private Hessian2Input _hIn;
  
  private int _addressCacheIndex;
  private String []_addressCacheRing = new String[256];

  public HmtpReader()
  {
    _hIn = new Hessian2Input();
    _hIn.allow("com.caucho.*");
  }

  public HmtpReader(InputStream is)
  {
    this();
    
    init(is);
  }
  
  public void setId(String id)
  {
    _id = id;
  }

  public void init(InputStream is)
  {
    _hIn.reset();
    
    /*
    _is = is;
    
    if (log.isLoggable(Level.FINEST)) {
      HessianDebugInputStream hIs
        = new HessianDebugInputStream(is, log, Level.FINEST);
      
      hIs.startStreaming();
      is = hIs;
    }
    
    private Hessian2Input _hIn = new Hessian2Input();
    _in = new Hessian2StreamingInput(is);
    */
  }

  /**
   * Returns true if buffered read data is already available, i.e.
   * the read stream will not block.
   */
  public boolean isDataAvailable()
  {
    return _in != null && _in.isDataAvailable();
  }

  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  public boolean readPacket(InputStream is,
                            MessageStream actorStream)
    throws IOException
  {
    if (actorStream == null)
      throw new IllegalStateException("HmtpReader.readPacket requires a valid ActorStream for callbacks");

    Hessian2Input hIn = _hIn;
    
    hIn.initPacket(is);

    int type = hIn.readInt();
    String to = readAddress(hIn);
    String from = readAddress(hIn);
    
    switch (HmtpPacketType.TYPES[type]) {
    case MESSAGE:
      {
        Serializable value = (Serializable) hIn.readObject();

        if (log.isLoggable(Level.FINEST)) {
          log.finest(this + " message " + value
                    + " {to:" + to + ", from:" + from + "}");
        }
        
        actorStream.message(to, from, value);

        break;
      }

    case MESSAGE_ERROR:
      {
        Serializable value = (Serializable) hIn.readObject();
        BamError error = (BamError) hIn.readObject();

        if (log.isLoggable(Level.FINEST)) {
          log.finest(this + " messageError " + error + " " + value
                    + " {to:" + to + ", from:" + from + "}");
        }

        actorStream.messageError(to, from, value, error);

        break;
      }

    case QUERY:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();

        if (log.isLoggable(Level.FINEST)) {
          log.finest(this + " query " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.query(id, to, from, value);

        break;
      }

    case QUERY_RESULT:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();

        if (log.isLoggable(Level.FINEST)) {
          log.finest(this + " queryResult " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryResult(id, to, from, value);

        break;
      }

    case QUERY_ERROR:
      {
        long id = hIn.readLong();
        Serializable value = (Serializable) hIn.readObject();
        BamError error = (BamError) hIn.readObject();

        if (log.isLoggable(Level.FINEST)) {
          log.finest(this + " queryError " + error + " " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryError(id, to, from, value, error);

        break;
      }

    default:
      throw new UnsupportedOperationException("ERROR: " + HmtpPacketType.TYPES[type]);
    }

    return true;
  }
  
  private String readAddress(Hessian2Input hIn)
    throws IOException
  {
    Object value = hIn.readObject();
    
    if (value == null)
      return null;
    else if (value instanceof String) {
      String address = (String) value;
      _addressCacheRing[_addressCacheIndex] = address;
      
      _addressCacheIndex = (_addressCacheIndex + 1) % _addressCacheRing.length;
      
      return address;
    }
    else if (value instanceof Integer) {
      int index = (Integer) value;
      
      return _addressCacheRing[index];
    }
    else
      throw new IllegalStateException(String.valueOf(value));
  }

  public void close()
  {
    try {
      Hessian2StreamingInput in = _in;
      _in = null;

      if (in != null)
        in.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    // _client.close();
  }

  @Override
  public String toString()
  {
    if (_id != null)
      return getClass().getSimpleName() + "[" + _id + "]";
    else
      return getClass().getSimpleName() + "[" + _is + "]";
  }
}

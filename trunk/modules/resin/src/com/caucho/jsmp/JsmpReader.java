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

package com.caucho.jsmp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.stream.MessageStream;
import com.caucho.hmtp.HmtpPacketType;
import com.caucho.json.JsonInput;

/**
 * JmtpReader stream handles client packets received from the server.
 */
public class JsmpReader {
  private static final Logger log
    = Logger.getLogger(JsmpReader.class.getName());

  private static final HashMap<String,HmtpPacketType> _typeMap
    = new HashMap<String,HmtpPacketType>();

  private InputStream _is;
  private JsonInput _in;

  public JsmpReader()
  {
  }

  public JsmpReader(InputStream is)
  {
    init(is);
  }

  public void init(InputStream is)
  {
    _is = is;
    _in = new JsonInput(is);
  }

  /**
   * Reads the next HMTP packet from the stream, returning false on
   * end of file.
   */
  public boolean readPacket(MessageStream actorStream)
    throws IOException
  {
    if (actorStream == null)
      throw new IllegalStateException("HmtpReader.readPacket requires a valid ActorStream for callbacks");

    JsonInput in = _in;

    if (in == null)
      return false;

    String type = readString();
    String to = readString();
    String from = readString();
    String payloadType = readString();
    Class payloadClass = getPayloadClass(payloadType);

    HmtpPacketType packetType = _typeMap.get(type);

    if (packetType == null) {
      throw new IllegalStateException("'" + type + "' is an unknown packet type");
    }

    switch (packetType) {
    case MESSAGE:
      {
        Serializable value = (Serializable) in.readObject(payloadClass);
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " message " + value
                    + " {to:" + to + ", from:" + from + "}");
        }

        actorStream.message(to, from, value);

        break;
      }

    case MESSAGE_ERROR:
      {
        Serializable value = (Serializable) in.readObject();
        BamError error = (BamError) in.readObject(BamError.class);
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " messageError " + error + " " + value
                    + " {to:" + to + ", from:" + from + "}");
        }

        actorStream.messageError(to, from, value, error);

        break;
      }

    case QUERY:
      {
        long id = in.readLong();
        Serializable value = (Serializable) in.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " query " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.query(id, to, from, value);

        break;
      }

    case QUERY_RESULT:
      {
        long id = in.readLong();
        Serializable value = (Serializable) in.readObject();
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " queryResult " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryResult(id, to, from, value);

        break;
      }

    case QUERY_ERROR:
      {
        long id = in.readLong();
        Serializable value = (Serializable) in.readObject();
        BamError error = (BamError) in.readObject(BamError.class);
        in.endPacket();

        if (log.isLoggable(Level.FINER)) {
          log.finer(this + " queryError " + error + " " + value
                    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
        }

        actorStream.queryError(id, to, from, value, error);

        break;
      }

    default:
      throw new UnsupportedOperationException("ERROR: " + type);
    }

    return true;
  }

  public String readString()
    throws IOException
  {
    InputStream is = _is;

    if (is == null)
      return null;

    int ch;

    while ((ch = is.read()) >= 0 && Character.isWhitespace(ch)) {
    }

    if (ch < 0 || ch == 0xff)
      return null;

    StringBuilder sb = new StringBuilder();
    sb.append((char) ch);

    while ((ch = is.read()) >= 0
           && ! Character.isWhitespace(ch)) {
      sb.append((char) ch);
    }

    return sb.toString();
  }

  public boolean startPacket()
    throws IOException
  {
    if (_is == null)
      return false;

    int ch;

    while ((ch = _is.read()) >= 0 && Character.isWhitespace((char) ch)) {
    }

    if (ch < 0)
      return false;
    else if (ch == 0)
      return true;
    else
      throw new IOException("0x" + Integer.toHexString(ch) + " is an illegal JmtpPacket start");
  }

  public void close()
  {
    try {
      JsonInput in = _in;
      _in = null;

      if (in != null)
        in.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    // _client.close();
  }

  protected Class getPayloadClass(String type)
  {
    if ("String".equals(type))
      return null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class cl = Class.forName(type, false, loader);

      return cl;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _is + "]";
  }

  static {
    _typeMap.put("message", HmtpPacketType.MESSAGE);
    _typeMap.put("message_error", HmtpPacketType.MESSAGE_ERROR);

    _typeMap.put("query", HmtpPacketType.QUERY);
    _typeMap.put("result", HmtpPacketType.QUERY_RESULT);
    _typeMap.put("query_error", HmtpPacketType.QUERY_ERROR);
  }
}

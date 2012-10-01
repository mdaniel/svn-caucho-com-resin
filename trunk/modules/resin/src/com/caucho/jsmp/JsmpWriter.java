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
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.ProtocolException;
import com.caucho.json.JsonOutput;

/**
 * JmtpWriteStream writes JMTP packets to an OutputStream.
 */
public class JsmpWriter
{
  private static final Logger log
  = Logger.getLogger(JsmpWriter.class.getName());

  private JsonOutput _out = new JsonOutput();

  public JsmpWriter()
  {
  }

  //
  // message
  //

  /**
   * JMTP unidirectional message
   *
   * <code><pre>
   * ["message",
   *  "to@to-host.com",
   *  "from@from-host.com",
   *  "message-class",
   *  "json-payload"]
   * </pre></code>
   */
  public void message(PrintWriter os, 
                      String to, 
                      String from, 
                      Serializable value)
  {
    try {
      JsonOutput out = _out;
      
      if (out == null)
        return;
      
      out.init(os);
      
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " message " + value
                  + " {to:" + to + ", from:" + from + "}");
      }
      

      os.print("[\"message\"");
      
      if (to != null) {
        os.print(",\"");
        os.print(to);
        os.print("\"");
      }
      else {
        os.print(",null");
      }
        
      if (from != null) {
        os.print(",\"");
        os.print(from);
        os.print("\"");
      }
      else {
        os.print(",null");
      }
      
      os.print(",\"");
      writeType(os, value);
      os.print("\"");

      os.print(",");
      out.writeObject(value);
      out.flushBuffer();
      os.write("]");
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * JMTP unidirectional message
   *
   * <code><pre>
   * ["message_error",
   *  "to@to-host.com",
   *  "from@from-host.com",
   *  "com.example.MessageType",
   *  json-payload,
   *  json-error]
   * </pre></code>
   */
  public void messageError(PrintWriter os,
                           String to,
                           String from,
                           Serializable value,
                           BamError error)
  {
    try {
      JsonOutput out = _out;

      if (out == null)
        return;
      
      out.init(os);
      
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " messageError " + value
                  + " {to:" + to + ", from:" + from + "}");
      }

      os.print("[\"message_error\",\"");

      os.print(to);
        
      os.write("\",\"");

      os.print(from);
      
      os.write("\",\"");

      writeType(os, value);
        
      os.write("\",");

      out.writeObject(value);
      out.flushBuffer();
      os.write(",");
      out.writeObject(error);
      out.flushBuffer();
      os.write("]");
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  //
  // query
  //

  /**
   * Low-level query
   */
  public void query(PrintWriter os,
                    long id,
                    String to,
                    String from,
                    Serializable value)
  {
    try {
      JsonOutput out = _out;

      if (out == null)
        return;
      
      out.init(os);
      
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " query " + value
                  + " {id: " + id + ", to:" + to + ", from:" + from + "}");
      }

      os.print("[\"query\"");
        
      if (to != null) {
        os.print(",\"");
        os.print(to);
        os.print("\"");
      }
      else {
        os.print(",null");
      }

      if (from != null) {
        os.print(",\"");
        os.print(from);
        os.print("\"");
      }
      else {
        os.print(",null");
      }

      os.print(",");
      os.print(id);
      
      os.write(",\"");
      writeType(os, value);

      os.print("\",");
      out.writeObject(value);
      out.flushBuffer();

      os.print("]");
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Low-level query
   */
  public void queryResult(PrintWriter os,
                          long id,
                          String to,
                          String from,
                          Serializable value)
  {
    try {
      JsonOutput out = _out;

      if (out == null)
        return;
      
      out.init(os);
      
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " result " + value
                  + " {id: " + id + ", to:" + to + ", from:" + from + "}");
      }

      os.print("[\"result\"");

      if (to != null) {
        os.print(",\"");
        os.print(to);
        os.print("\"");
      }
      else {
        os.print(",null");
      }

      if (from != null) {
        os.print(",\"");
        os.print(from);
        os.print("\"");
      }
      else {
        os.print(",null");
      }

      os.print(",");
      os.print(id);

      os.write(",\"");
      writeType(os, value);
      
      os.print("\",");
      out.writeObject(value);
      out.flushBuffer();

      os.print("]");
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  /**
   * Low-level query
   */
  public void queryError(PrintWriter os,
                         long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    try {
      JsonOutput out = _out;

      if (out == null)
        return;
      
      out.init(os);
      
      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " query_error " + error + "\n  " + payload + "\n  "
                  + " {id: " + id + ", to:" + to + ", from:" + from + "}");
      }

      os.print("[\"query_error\"");

      if (to != null) {
        os.print(",\"");
        os.print(to);
        os.print("\"");
      }
      else {
        os.print(",null");
      }

      if (from != null) {
        os.print(",\"");
        os.print(from);
        os.print("\"");
      }

      os.print(",");
      os.print(id);


      os.write(",\"");
      writeType(os, payload);
      
      os.print("\",");
      out.writeObject(payload);
      out.flushBuffer();

      os.print(",");
      out.writeObject(error);
      out.flushBuffer();

      os.print("]");
    } catch (IOException e) {
      throw new ProtocolException(e);
    }
  }

  private void writeType(PrintWriter os, Object value)
    throws IOException
  {
    if (value == null) {
      os.print("null");
      return;
    }

    Class<?> cl = value.getClass();

    if (cl == String.class) {
      os.print("String");
    }
    else if (cl.getName().startsWith("java.")) {
      os.print("Object\n");
    }
    else {
      os.print(value.getClass().getName());
    }
  }

  public void flush()
    throws IOException
  {
    JsonOutput out = _out;

    if (out != null) {
      out.flush();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

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

import com.caucho.bam.BamError;
import com.caucho.bam.ProtocolException;
import com.caucho.bam.actor.AbstractActor;
import com.caucho.bam.broker.AbstractBroker;
import com.caucho.remote.websocket.WebSocketContextStreamImpl;
import com.caucho.util.IoUtil;
import com.caucho.vfs.WriteStream;
import com.caucho.websocket.WebSocketContext;

/**
 * JsmpWriteStream writes JSMP packets to an OutputStream.
 */
public class JsmpWebSocketWriter extends AbstractActor
{
  private String _address;
  
  private WebSocketContext _wsContext;

  private JsmpWriter _jsOut;

  public JsmpWebSocketWriter(WriteStream out)
    throws IOException
  {
    this(new WebSocketContextStreamImpl(out));
  }

  public JsmpWebSocketWriter(WebSocketContext context)
    throws IOException
  {
    _wsContext = context;
    _jsOut = new JsmpWriter();
  }
  
  /**
   * The address of the stream
   */
  @Override
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
   * @param to the address of the target actor
   * @param from the address of the source actor
   * @param payload the message payload
   */
  @Override
  public void message(String to, 
                      String from, 
                      Serializable payload)
  {
    PrintWriter out = null;
    
    try {
      out = _wsContext.startTextMessage();
      
      _jsOut.message(out, to, from, payload);
    } catch (IOException e) {
      throw new ProtocolException(e);
    } finally {
      IoUtil.close(out);
    }
  }

  /**
   * Sends a message error to a given address
   * 
   * @param to the address of the target actor
   * @param from the address of the source actor
   * @param payload the message payload
   * @param error the message error
   */
  @Override
  public void messageError(String to, 
                           String from, 
                           Serializable payload,
                           BamError error)
  {
    PrintWriter out = null;
    
    try {
      out = _wsContext.startTextMessage();
      
      _jsOut.messageError(out, to, from, payload, error);
    } catch (IOException e) {
      throw new ProtocolException(e);
    } finally {
      IoUtil.close(out);
    }
  }

  /**
   * Sends a queryGet to a given address
   * 
   * @param id the query id
   * @param to the address of the target actor
   * @param from the address of the source actor
   * @param payload the message payload
   */
  @Override
  public void query(long id,
                    String to, 
                    String from, 
                    Serializable payload)
  {
    PrintWriter out = null;
    
    try {
      out = _wsContext.startTextMessage();
      
      _jsOut.query(out, id, to, from, payload);
    } catch (IOException e) {
      throw new ProtocolException(e);
    } finally {
      IoUtil.close(out);
    }
  }

  /**
   * Sends a queryResult to a given address
   * 
   * @param id the query id
   * @param to the address of the target actor
   * @param from the address of the source actor
   * @param payload the message payload
   */
  @Override
  public void queryResult(long id,
                          String to, 
                          String from, 
                          Serializable payload)
  {
    PrintWriter out = null;
    
    try {
      out = _wsContext.startTextMessage();
      
      _jsOut.queryResult(out, id, to, from, payload);
    } catch (IOException e) {
      throw new ProtocolException(e);
    } finally {
      IoUtil.close(out);
    }
  }

  /**
   * Sends a query error to a given address
   * 
   * @param id the query identifier
   * @param to the address of the target actor
   * @param from the address of the source actor
   * @param payload the message payload
   * @param error the message error
   */
  @Override
  public void queryError(long id,
                         String to, 
                         String from, 
                         Serializable payload,
                         BamError error)
  {
    PrintWriter out = null;
    
    try {
      out = _wsContext.startTextMessage();
      
      _jsOut.queryError(out, id, to, from, payload, error);
    } catch (IOException e) {
      throw new ProtocolException(e);
    } finally {
      IoUtil.close(out);
    }
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.ActorStream#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * 
   */
  public void flush()
  {
    /*
    try {
      _wsOut.flush();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
    */
  }
  
  public void close()
  {
    
  }

}

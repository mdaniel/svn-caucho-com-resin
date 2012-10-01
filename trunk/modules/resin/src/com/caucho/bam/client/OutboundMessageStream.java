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

package com.caucho.bam.client;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;

/**
 * HMTP client protocol
 */
public class OutboundMessageStream implements MessageStream {
  private static final Logger log 
    = Logger.getLogger(OutboundMessageStream.class.getName());
  
  private final LinkConnectionFactory _linkFactory;
  
  private final Broker _inboundBroker;
  
  private final AtomicReference<LinkConnection> _connRef
    = new AtomicReference<LinkConnection>();
  
  public OutboundMessageStream(LinkConnectionFactory linkFactory, 
                             Broker inboundBroker)
  {
    if (linkFactory == null)
      throw new NullPointerException();
      
    if (inboundBroker == null)
      throw new NullPointerException();
    
    _linkFactory = linkFactory;
    _inboundBroker = inboundBroker;
  }
  
  @Override
  public String getAddress()
  {
    LinkConnection conn = _connRef.get();
    
    if (conn != null)
      return conn.getAddress();
    else
      return null;
  }
  
  @Override
  public Broker getBroker()
  {
    return _inboundBroker;
  }
  
  @Override
  public boolean isClosed()
  {
    return _linkFactory.isClosed();
  }
  
  @Override
  public void message(String to, String from, Serializable payload)
  {
    try {
      getLink().message(to, from, payload);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      getBroker().messageError(from, to, payload, BamError.create(e));
    }
  }
  
  @Override
  public void messageError(String to, 
                           String from, 
                           Serializable payload,
                           BamError error)
  {
    try {
      getLink().messageError(to, from, payload, error);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    try {
      MessageStream link = getLink();
      
      if (link != null)
        link.query(id, to, from, payload);
      else
        getBroker().queryError(id, from, to, payload, new BamError("link closed"));
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      getBroker().queryError(id, from, to, payload, BamError.create(e));
    }
  }
  
  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    try {
      getLink().queryResult(id, to, from, payload);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  @Override
  public void queryError(long id,
                         String to, 
                         String from, 
                         Serializable payload,
                           BamError error)
  {
    try {
      getLink().queryError(id, to, from, payload, error);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  private MessageStream getLink()
  {
    LinkConnection conn = _connRef.get();
    
    if (conn != null && conn.isClosed())
      conn = null;
    
    if (conn == null) {
      conn = _linkFactory.open(getBroker());
      
      if (! _connRef.compareAndSet(null, conn)) {
        // conn.close();
      }
      
      conn = _connRef.get();
    }
    
    return conn.getOutboundStream();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _linkFactory + "]";
  }
}

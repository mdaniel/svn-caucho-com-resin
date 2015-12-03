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

package com.caucho.bam.stream;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.Broker;


/**
 * Abstract implementation of a BAM filter.  The default operation
 * of most methods is to forward the request to the next stream.
 */
abstract public class AbstractMessageStreamFilter implements MessageStream
{
  abstract protected MessageStream getNext();
 
  /**
   * Returns the address of the final actor
   */
  @Override
  public String getAddress()
  {
    return getNext().getAddress();
  }
  
  /**
   * Returns the broker of the final actor.
   */
  @Override
  public Broker getBroker()
  {
    return getNext().getBroker();
  }
  
  /**
   * Sends a unidirectional message
   * 
   * @param to the target address
   * @param from the source address
   * @param payload the message payload
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    getNext().message(to, from, payload);
  }
  
  /**
   * Sends a unidirectional message error
   * 
   * @param to the target address
   * @param from the source address
   * @param payload the message payload
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    getNext().messageError(to, from, payload, error);
  }
  
  @Override
  public void query(long id,
                    String to,
                    String from,
                    Serializable payload)
  {
    getNext().query(id, to, from, payload);
  }
  
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    getNext().queryResult(id, to, from, payload);
  }
  
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    getNext().queryError(id, to, from, payload, error);
  }
  
  @Override
  public boolean isClosed()
  {
    return getNext().isClosed();
  }

  /**
   * Closes the filter, but not the child by default.
   */
  public void close()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getNext() + "]";
  }
}

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

import com.caucho.bam.broker.Broker;

/**
 * Base ActorStream implementation using introspection and
 * {@link com.caucho.bam.Message @Message} annotations to simplify
 * Actor development.
 *
 * <h2>Message Handling</h2>
 *
 * To handle a message, create a method with the proper signature for
 * the expected payload type and
 * annotate it with {@link com.caucho.bam.Message @Message}.  To send
 * a response message or query, use <code>getBrokerStream()</code> or
 * <code>getClient()</code>.
 *
 * <code><pre>
 * @Message
 * public void myMessage(String to, String from, MyPayload payload);
 * </pre></code>
 */
public class FallbackMessageStream extends NullMessageStream
{
  private Class<?> _actorClass;
  
  public FallbackMessageStream(String address,
                             Broker broker,
                             Class<?> actorClass)
  {
    super(address, broker);
    
    _actorClass = actorClass;
  }
  
  public FallbackMessageStream(MessageStream actorStream)
  {
    this(actorStream.getAddress(), actorStream.getBroker(), actorStream.getClass());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "," + _actorClass.getSimpleName() + "]";
  }
}

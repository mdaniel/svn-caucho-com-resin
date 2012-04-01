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

package com.caucho.amp.actor;

import java.util.logging.Logger;

import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.mailbox.AmpMailboxFactory;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpHeaders;
import com.caucho.amp.stream.AmpStream;
import com.caucho.amp.stream.NullEncoder;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.ExpandableArray;
import com.caucho.util.WeakAlarm;

/**
 * Handles the context for an actor, primarily including its
 * query map.
 */
public final class AmpProxyActor implements AmpActor {
  private static final Logger log
    = Logger.getLogger(AmpProxyActor.class.getName());

  private final String _address;

  private final ActorContextImpl _actorContext;

  public AmpProxyActor(String address, 
                       AmpMailboxFactory mailboxFactory)
  {
    _address = address;
    
    _actorContext = new ActorContextImpl(this, mailboxFactory);
  }

  @Override
  public String getAddress()
  {
    return _address;
  }
  
  public ActorContextImpl getActorContext()
  {
    return _actorContext;
  }

  @Override
  public void send(String to, String from, AmpHeaders headers,
                   AmpEncoder encoder, String methodName, Object... args)
  {
    log.warning(this + " send from " + from);
  }

  @Override
  public void error(String to, String from, AmpHeaders headers,
                    AmpEncoder encoder, AmpError error)
  {
    log.finer(this + " error from " + from);
  }

  @Override
  public void query(long id, 
                    String to, String from, 
                    AmpHeaders headers,
                    AmpEncoder encoder, 
                    String methodName, Object... args)
  {
    log.warning(this + " query from " + from);
  }

  @Override
  public void queryResult(long id, 
                          String to, 
                          String from, 
                          AmpHeaders headers,
                          AmpEncoder encoder,
                          Object result)
  {
    log.warning(this + " query error");
  }

  @Override
  public void queryError(long id, 
                         String to, 
                         String from, 
                         AmpHeaders headers,
                         AmpEncoder encoder, 
                         AmpError error)
  {
    log.warning(this + " query error");
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }
}

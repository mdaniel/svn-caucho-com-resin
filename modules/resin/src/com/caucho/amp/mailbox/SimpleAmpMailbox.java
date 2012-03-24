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

package com.caucho.amp.mailbox;

import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpHeaders;
import com.caucho.amp.stream.AmpStream;

/**
 * Mailbox for an actor
 */
public class SimpleAmpMailbox implements AmpMailbox
{
  private AmpActor _actor;
  
  public SimpleAmpMailbox(AmpActor actor)
  {
    _actor = actor;
  }
  
  /**
   * Returns the delegated actor stream for the actor itself.
   */
  @Override
  public AmpStream getActorStream()
  {
    return _actor;
  }

  @Override
  public String getAddress()
  {
    return getActorStream().getAddress();
  }

  @Override
  public void send(String to, 
                   String from, 
                   AmpHeaders headers,
                   AmpEncoder encoder, 
                   String methodName, 
                   Object... args)
  {
    getActorStream().send(to, from, headers, encoder, methodName, args);
  }

  @Override
  public void error(String to, 
                    String from, 
                    AmpHeaders headers,
                    AmpEncoder encoder, 
                    AmpError error)
  {
    getActorStream().error(to, from, headers, encoder, error);
  }

  @Override
  public void query(long id, 
                    String to, 
                    String from, 
                    AmpHeaders headers,
                    AmpEncoder encoder, 
                    String methodName, 
                    Object... args)
  {
    getActorStream().query(id, to, from, headers, encoder,
                           methodName, args);
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.stream.AmpStream#queryResult(long, java.lang.String, java.lang.String, com.caucho.amp.stream.AmpHeaders, com.caucho.amp.stream.AmpEncoder, java.lang.Object)
   */
  @Override
  public void queryResult(long id, String to, String from, AmpHeaders headers,
                          AmpEncoder encoder, Object result)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.amp.stream.AmpStream#queryError(long, java.lang.String, java.lang.String, com.caucho.amp.stream.AmpHeaders, com.caucho.amp.stream.AmpEncoder, com.caucho.amp.stream.AmpError)
   */
  @Override
  public void queryError(long id, String to, String from, AmpHeaders headers,
                         AmpEncoder encoder, AmpError error)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  /**
   * Closes the mailbox
   */
  @Override
  public void close()
  {
    
  }
}

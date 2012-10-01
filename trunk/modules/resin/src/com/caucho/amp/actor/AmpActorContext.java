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

import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.mailbox.AmpMailbox;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpStream;

/**
 * Context for an actor.
 */
abstract public class AmpActorContext
{
  private static final ThreadLocal<AmpActorContext> _currentContext
    = new ThreadLocal<AmpActorContext>();
  
  public static final AmpActorContext getCurrent()
  {
    return _currentContext.get();
  }
  
  public static final AmpActorContext getCurrent(AmpActorContext systemContext)
  {
    AmpActorContext context = _currentContext.get();
    
    if (context != null)
      return context;
    else
      return systemContext;
  }
 
  abstract public String getAddress();
  
  abstract public AmpMailbox getMailbox();
  
  abstract public AmpActorRef getActorRef();
  
  abstract public AmpStream getStream();
  
  abstract public AmpMethodRef getMethod(String methodName, AmpEncoder encoder);
  
  public final AmpActorContext beginCurrentActor()
  {
    final AmpActorContext prev = _currentContext.get();
    
    _currentContext.set(this);
    
    return prev;
  }
  
  public final void endCurrentActor(AmpActorContext prev)
  {
    _currentContext.set(prev);
  }

  abstract public void query(AmpMethodRef methodRef,
                             Object[] args,
                             AmpQueryCallback cb, 
                             long timeout);

  public void send(AmpMethodRef methodRef, Object[] args)
  {
    methodRef.send(getActorRef(), args);
  }
}

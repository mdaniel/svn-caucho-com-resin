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

package com.caucho.amp.router;

import com.caucho.amp.AmpManager;
import com.caucho.amp.AmpQueryCallback;
import com.caucho.amp.actor.AbstractActorRef;
import com.caucho.amp.actor.ActorContextImpl;
import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.AmpMethodRef;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.NullEncoder;

/**
 * Sender for an actor ref.
 */
public class FirstActorRouter extends AbstractActorRef
{
  private final AmpActorContext _systemContext;
  private final AmpActorRef []_actors;
  
  public FirstActorRouter(AmpManager manager, AmpActorRef ...actors)
  {
    _systemContext = manager.getSystemContext();
    _actors = actors;
  }

  @Override
  public String getAddress()
  {
    return _actors[0].getAddress();
  }

  @Override
  public AmpMethodRef getMethod(String methodName, AmpEncoder encoder)
  {
    AmpMethodRef []methods = new AmpMethodRef[_actors.length];
    
    for (int i = 0; i < methods.length; i++ ) {
      methods[i] = _actors[i].getMethod(methodName, encoder);
    }
    
    return new FirstMethodRef(_systemContext, methods);
  }
  
  static class FirstMethodRef implements AmpMethodRef {
    private final AmpActorContext _systemContext;
    private final AmpMethodRef []_methods;
    
    FirstMethodRef(AmpActorContext systemContext,
                   AmpMethodRef []methods)
    {
      _systemContext = systemContext;
      _methods = methods;
    }

    @Override
    public void send(AmpActorRef from, Object... args)
    {
      for (int i = 0; i < _methods.length; i++) {
        _methods[i].send(from, args);
        
        return;
      }
    }

    @Override
    public void query(long id, AmpActorRef from, Object... args)
    {
      new FirstMethodCallback(_systemContext, id, from, _methods, args).start();
    }
  }
  
  static class FirstMethodCallback implements AmpQueryCallback {
    private final AmpActorContext _systemContext;
    private final long _id;
    private final AmpActorRef _from;
    private final AmpMethodRef []_methods;
    private final Object []_args;
    
    private int _index;
    
    FirstMethodCallback(AmpActorContext systemContext,
                        long id, 
                        AmpActorRef from,
                        AmpMethodRef []methods,
                        Object []args)
    {
      _systemContext = systemContext;
      _id = id;
      _from = from;
      _methods = methods;
      _args = args;
      
    }
    
    void start()
    {
      if (! nextQuery()) {
        _from.queryError(_id, _from, NullEncoder.ENCODER, new AmpError());
      }
    }
    
    @Override
    public void onQueryResult(AmpActorRef to, AmpActorRef from, Object result)
    {
      _from.reply(_id, from, NullEncoder.ENCODER, result);
    }

    @Override
    public void onQueryError(AmpActorRef to, AmpActorRef from, AmpError error)
    {
      if (! nextQuery()) {
        _from.queryError(_id, from, NullEncoder.ENCODER, error);
      }
    }
    
    private boolean nextQuery()
    {
      while (_index < _methods.length) {
        int index = _index;
        _index = index + 1;
        
        AmpActorContext cxt = AmpActorContext.getCurrent(_systemContext);
        
        cxt.query(_methods[index], _args, this, 5000L);
        return true;
      }
      
      return false;
    }
  }
}

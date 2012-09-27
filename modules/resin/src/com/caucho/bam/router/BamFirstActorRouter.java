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

package com.caucho.bam.router;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.BamActorRef;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;

/**
 * Sends a message to the first available actor.
 */
public class BamFirstActorRouter extends AbstractBamRouter
{
  private final Broker _broker;
  private final ActorSender _sender;
  private final BamActorRef []_actors;
  
  private final long _actorTimeout;
  
  public BamFirstActorRouter(ActorSender sender,
                             long timeout,
                             BamActorRef ...actors)
  {
    _broker = sender.getBroker();
    _sender = sender;
    _actors = actors;
    
    _actorTimeout = timeout;
  }

  @Override
  public String getAddress()
  {
    return getActors()[0].getAddress();
  }
  
  protected BamActorRef []getActors()
  {
    return _actors;
  }
  
  @Override
  public boolean isActive()
  {
    for (BamActorRef actor : getActors()) {
      if (actor.isActive()) {
        return true;
      }
    }
    
    return false;
  }
  
  @Override
  public ActorSender getSender()
  {
    return _sender;
  }

  @Override
  public void message(String from, Serializable payload)
  {
    for (BamActorRef actor : _actors) {
      if (actor.isActive()) {
        actor.message(from, payload);
        return;
      }
    }
  }

  @Override
  public void query(long id, String from, Serializable payload)
  {
    new FirstMethodCallback(id, from, payload, _actorTimeout).start();
  }
  
  class FirstMethodCallback implements QueryCallback {
    private final long _id;
    private final String _from;
    private final Serializable _payload;
    private final long _actorTimeout;
    
    private int _index;
    
    FirstMethodCallback(long id, 
                        String from,
                        Serializable payload,
                        long actorTimeout)
    {
      _id = id;
      _from = from;
      _payload = payload;
      _actorTimeout = actorTimeout;
    }
    
    void start()
    {
      if (! nextQuery()) {
        fail();
      }
    }
    
    private void fail()
    {
      _broker.queryError(_id, _from, _sender.getAddress(),
                         _payload, 
                         new BamError(BamError.TYPE_CANCEL,
                                      BamError.REMOTE_CONNECTION_FAILED,
                                      "no valid actors"));

      /*
      _broker.queryResult(_id, _from, _sender.getAddress(), null);
      */
    }

    @Override
    public void onQueryResult(String to, String from, Serializable payload)
    {
      _broker.queryResult(_id, _from, from, payload);
    }

    @Override
    public void onQueryError(String to, String from, 
                             Serializable payload,
                             BamError error)
    {
      if (! nextQuery()) {
        _broker.queryError(_id, _from, from, payload, error);
      }
    }
    
    private boolean nextQuery()
    {
      while (_index < _actors.length) {
        int index = _index;
        _index = index + 1;
        
        BamActorRef actor = _actors[index];
        
        if (actor.isActive()) {
          _sender.query(actor.getAddress(), _payload, this, _actorTimeout);
          return true;
        }
      }
      
      return false;
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _sender + "," + _from + "," + _payload + "]";
    }
  }
}

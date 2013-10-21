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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.BamActorRef;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.query.QueryCallback;

/**
 * Sends a message to the first available actor.
 */
public class BamAllActorRouter extends AbstractBamRouter
{
  private static final Logger log
    = Logger.getLogger(BamAllActorRouter.class.getName());
  
  private final Broker _broker;
  private final ActorSender _sender;
  private final BamActorRef []_actors;
  private final boolean _isForce;
  
  public BamAllActorRouter(ActorSender sender,
                           boolean isForce,
                           BamActorRef ...actors)
  {
    _broker = sender.getBroker();
    _sender = sender;
    _isForce = isForce;
    _actors = actors;
  }

  @Override
  public String getAddress()
  {
    return getActors()[0].getAddress();
  }
  
  @Override
  public boolean isActive()
  {
    if (_isForce) {
      return true;
    }
    
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
  
  protected BamActorRef []getActors()
  {
    return _actors;
  }

  @Override
  public void message(String from, Serializable payload)
  {
    for (BamActorRef actor : getActors()) {
      if (actor.isActive() || _isForce) {
        actor.message(from, payload);
      }
    }
  }

  @Override
  public void query(long id, String from, Serializable payload)
  {
    BamActorRef []actors = getActors();
    
    AllMethodScoreboard scoreboard
      = new AllMethodScoreboard(actors, id, from, payload);
    
    for (int i = 0; i < actors.length; i++) {
      BamActorRef actor = actors[i];
      
      if (actor != null && (actor.isActive() || _isForce)) {
        _sender.query(actor, payload, new AllMethodCallback(scoreboard, i));
      }
      else {
        // cloud/0350
        scoreboard.completeNotActive(i, actor);
      }
    }
    
    if (actors.length == 0) {
      scoreboard.completeEmpty();
    }
  }
  
  private class AllMethodScoreboard {
    private final long _id;
    private final String _from;
    private final Serializable _payload;
    
    private final boolean []_isComplete;
    
    private final AtomicBoolean _isReplySent = new AtomicBoolean();
    private final AtomicReference<Serializable> _result
    = new AtomicReference<Serializable>();
    private final AtomicReference<BamError> _error
      = new AtomicReference<BamError>();
    
    AllMethodScoreboard(BamActorRef []actors,
                        long id, 
                        String from,
                        Serializable payload)
    {
      _id = id;
      _from = from;
      _payload = payload;
      
      _isComplete = new boolean[actors.length];
      
      checkComplete();
    }
    
    private void complete(int index, Serializable result)
    {
      _result.compareAndSet(null, result);
      
      synchronized (_isComplete) {
        _isComplete[index] = true;
      }
      
      checkComplete();
    }
    
    private void complete(int index, BamError error)
    {
      _error.compareAndSet(null, error);
      
      synchronized (_isComplete) {
        _isComplete[index] = true;
      }
      
      checkComplete();
    }
    
    private void completeNotActive(int index, BamActorRef actor)
    {
      if (log.isLoggable(Level.FINEST)) {
        log.finer(this + " cannot contact " + actor + " because not active");
      }
      
      synchronized (_isComplete) {
        _isComplete[index] = true;
      }
      
      checkComplete();
    }
    
    private void completeEmpty()
    {
      if (_isComplete.length != 0)
        throw new IllegalStateException();
      
      checkComplete();
    }
    
    private void checkComplete()
    {
      synchronized (_isComplete) {
        for (int i = 0; i < _isComplete.length; i++) {
          if (! _isComplete[i])
            return;
        }
      }
      
      if (_isReplySent.compareAndSet(false, true)) {
        BamError error = _error.get();
        Serializable result = _result.get();
        
        if (error != null) {
          _broker.queryError(_id, _from, _sender.getAddress(), _payload, error);
        }
        else {
          _broker.queryResult(_id, _from, _sender.getAddress(), result);
        }
      }
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _id + "," + _from + "," + _sender + "]";
    }
  }
  
  class AllMethodCallback implements QueryCallback {
    private final AllMethodScoreboard _scoreboard;
    private final int _index;
    
    AllMethodCallback(AllMethodScoreboard scoreboard, int index)
    {
      _scoreboard = scoreboard;
      _index = index;
    }

    @Override
    public void onQueryResult(String to, String from, Serializable result)
    {
      _scoreboard.complete(_index, result);
    }

    @Override
    public void onQueryError(String to, String from, 
                             Serializable payload,
                             BamError error)
    {
      log.finer(this + " " + error);
      
      _scoreboard.complete(_index, error);
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _index + "," + _scoreboard + "]";
    }
  }
}

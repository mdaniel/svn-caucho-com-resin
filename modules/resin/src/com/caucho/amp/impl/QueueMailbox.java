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

package com.caucho.amp.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import com.caucho.amp.actor.AmpActorContext;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.mailbox.AbstractAmpMailbox;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.AmpStream;

/**
 * Mailbox for an actor
 */
public class QueueMailbox extends AbstractAmpMailbox
{
  private final LinkedBlockingQueue<Message> _queue
    = new LinkedBlockingQueue<Message>();
  private final AmpActorContext _actor;
  private final QueueWorker _worker;
  
  public QueueMailbox(AmpActorContext actor,
                      Executor executor)
  {
    _actor = actor;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    _worker = new QueueWorker(actor.getAddress(), executor, loader);
  }
  
  /**
   * Returns the delegated actor stream for the actor itself.
   */
  @Override
  public AmpStream getActorStream()
  {
    return _actor.getStream();
  }
  
  @Override
  public AmpActorContext getActorContext()
  {
    return _actor;
  }

  @Override
  public void send(final AmpActorRef to, 
                   final AmpActorRef from,
                   final AmpEncoder encoder, 
                   final String methodName, 
                   final Object... args)
  {
    _queue.offer(new SendMessage(to, from, encoder, methodName, args));

    _worker.wake();
  }

  @Override
  public void query(final long id, 
                    final AmpActorRef to, 
                    final AmpActorRef from,
                    final AmpEncoder encoder, 
                    final String methodName, 
                    final Object... args)
  {
    _queue.offer(new QueryMessage(id, to, from, encoder, methodName, args));
    
    _worker.wake();
  }

  @Override
  public void queryResult(final long id, 
                          final AmpActorRef to, 
                          final AmpActorRef from,
                          final AmpEncoder encoder, 
                          final Object result)
  {
    _queue.offer(new QueryReply(id, to, from, encoder, result));
    
    _worker.wake();
  }

  @Override
  public void queryError(final long id, 
                         final AmpActorRef to, 
                         final AmpActorRef from, 
                         final AmpEncoder encoder, 
                         final AmpError error)
  {
    _queue.offer(new QueryError(id, to, from, encoder, error));
  }

  @Override
  public void error(final AmpActorRef to, 
                    final AmpActorRef from,
                    final AmpEncoder encoder, 
                    final AmpError error)
  {
    _queue.offer(new ErrorMessage(to, from, encoder, error));
  }

  /**
   * Closes the mailbox
   */
  @Override
  public void close()
  {
    
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _actor + "]";
  }
  
  class QueueWorker extends AbstractActorWorker {
    QueueWorker(String name,
                Executor executor,
                ClassLoader loader)
    {
      super(name, executor, loader);
    }
    
    private void processQueue()
    {
      AmpStream stream = getActorStream();
      
      Message msg;
      
      while ((msg = _queue.poll()) != null) {
        msg.invoke(stream);
      }
    }
    
    @Override
    public void runTask()
    {
      AmpActorContext prev = _actor.beginCurrentActor();
      
      try {
        processQueue();
      } finally {
        _actor.endCurrentActor(prev);
      }
    }
  }
  
  static class Message {
    void invoke(AmpStream stream)
    {

    }
  }
  
  static class SendMessage extends Message {
    private final AmpActorRef _to;
    private final AmpActorRef _from;
    private final AmpEncoder _encoder;
    private final String _methodName;
    private final Object []_args;
    
    SendMessage(AmpActorRef to,
                AmpActorRef from,
                AmpEncoder encoder,
                String methodName,
                Object ...args)
    {
      _to = to;
      _from = from;
      _encoder = encoder;
      _methodName = methodName;
      _args = args;
    }

    @Override
    void invoke(AmpStream stream)
    {
      stream.send(_to, _from, _encoder, _methodName, _args);
    }
  }
  
  static class QueryMessage extends Message {
    private final long _id;
    private final AmpActorRef _to;
    private final AmpActorRef _from;
    private final AmpEncoder _encoder;
    private final String _methodName;
    private final Object []_args;
    
    QueryMessage(long id,
                 AmpActorRef to,
                 AmpActorRef from,
                 AmpEncoder encoder,
                 String methodName,
                 Object ...args)
    {
      _id = id;
      _to = to;
      _from = from;
      _encoder = encoder;
      _methodName = methodName;
      _args = args;
    }

    @Override
    void invoke(AmpStream stream)
    {
      stream.query(_id, _to, _from, _encoder, _methodName, _args);
    }
  }
  
  static class QueryReply extends Message {
    private final long _id;
    private final AmpActorRef _to;
    private final AmpActorRef _from;
    private final AmpEncoder _encoder;
    private final Object _result;
    
    QueryReply(long id,
               AmpActorRef to,
               AmpActorRef from,
               AmpEncoder encoder,
               Object result)
    {
      _id = id;
      _to = to;
      _from = from;
      _encoder = encoder;
      _result = result;
    }

    @Override
    void invoke(AmpStream stream)
    {
      stream.queryResult(_id, _to, _from, _encoder, _result);
    }
  }
  
  static class QueryError extends Message {
    private final long _id;
    private final AmpActorRef _to;
    private final AmpActorRef _from;
    private final AmpEncoder _encoder;
    private final AmpError _error;
    
    QueryError(long id,
               AmpActorRef to,
               AmpActorRef from,
               AmpEncoder encoder,
               AmpError error)
    {
      _id = id;
      _to = to;
      _from = from;
      _encoder = encoder;
      _error = error;
    }

    @Override
    void invoke(AmpStream stream)
    {
      stream.queryResult(_id, _to, _from, _encoder, _error);
    }
  }
  
  static class ErrorMessage extends Message {
    private final AmpActorRef _to;
    private final AmpActorRef _from;
    private final AmpEncoder _encoder;
    private final AmpError _error;
    
    ErrorMessage(AmpActorRef to,
                 AmpActorRef from,
                 AmpEncoder encoder,
                 AmpError error)
    {
      _to = to;
      _from = from;
      _encoder = encoder;
      _error = error;
    }

    @Override
    void invoke(AmpStream stream)
    {
      stream.error(_to, _from, _encoder, _error);
    }
  }
}

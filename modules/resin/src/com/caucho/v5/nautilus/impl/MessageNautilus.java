/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.nautilus.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
class MessageNautilus
{
  private final long _mid;
  //private MessageDataNode _data;

  private State _state = State.JOURNAL;
  private long _jdbcOid;

  private MessageNautilus _next;

  private byte []_buffer;
  private int _offset;
  private int _length;

  MessageNautilus(long mid, byte []buffer, int offset, int length)
  {
    _mid = mid;
    _buffer = buffer;
    _offset = offset;
    _length = length;
    
    _state = State.BUFFER;
  }

  MessageNautilus(long mid, int priority, long expireTime, long jdbcOid)
  {
    _mid = mid;
    _jdbcOid = jdbcOid;
    
    _state = State.STORE;
  }

  /**
   * @param sub
   */
   public void receive(QueueServiceLocal queue, ReceiverBrokerNautilus receiver)
   {
     if (_state == State.STORE) {
       //queue.getBroker().receiveJdbc(_jdbcOid, this, receiver);
       queue.receiveFromStore(_mid, this, receiver);
     }
     else if (_state == State.BUFFER) {
       ByteArrayInputStream bis = new ByteArrayInputStream(_buffer, _offset, _length);
       
       receiver.receive(this, bis, _length);
     }
     else {
       throw new IllegalStateException();
     }
     
     _state = _state.toReceive();
   }

   public InputStream openInputStream()
   {
     return new ByteArrayInputStream(_buffer, _offset, _length);
   }

   public void accepted(QueueServiceLocal queue)
   {
     //if (_state == State.STORE) {
       queue.getBroker().accepted(queue.getId(), _mid);
     //}

     /*
     if (_jdbcOid > 0) {
       queue.getBroker().acceptJdbc(_jdbcOid, this);
     }
     */
   }

   long getSequence()
   {
     return _mid;
   }

   boolean toSaving()
   {
     if (_state == State.JOURNAL) {
       _state = State.JOURNAL_SAVING;
       return true;
     }
     else {
       return false;
     }
   }

   void toSaved(long jdbcOid)
   {
     _state = State.STORE;

     _jdbcOid = jdbcOid;
   }

   MessageNautilus getNext()
   {
     return _next;
   }

   void setNext(MessageNautilus next)
   {
     _next = next;
   }

   @Override
   public String toString()
   {
     return getClass().getSimpleName() + "[" + Long.toHexString(_mid) + "]";
   }

   enum State {
     NULL,
     JOURNAL {
       State toReceive() { return RECEIVING_JOURNAL; }
     },
     JOURNAL_SAVING {
       State toReceive() { return RECEIVING_JOURNAL; }
     },
     STORE {
       State toReceive() { return RECEIVING_JDBC; }
     },      
     RECEIVING_JOURNAL,
     RECEIVING_JDBC,
     BUFFER {
       State toReceive() { return RECEIVING_BUFFER; }
     },
     RECEIVING_BUFFER {
     };
     
     State toReceive()
     {
       throw new UnsupportedOperationException(toString());
     }
     
   }
}

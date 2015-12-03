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

package com.caucho.message.common;

/**
 * counts message send/receive credit.
 * 
 * It is assumed that the methods are called in a thread safe manner
 */
public final class MessageReceiverCredit {
  private int _prefetch;
  
  private int _creditHighLimit;
  private int _creditLowLimit;
  private boolean _isHighUpdate;
  
  private volatile long _endpointSequence;
  private volatile long _clientSequence;
  
  private volatile long _creditEnd;
  
  public final int getPrefetch()
  {
    return _prefetch;
  }
  
  public final void setPrefetch(int prefetch)
  {
    _prefetch = prefetch;
    
    _creditHighLimit = (int) (prefetch * 0.75);
    _creditLowLimit = Math.min(Math.max(2, (int) (prefetch * 0.25)), prefetch);
  }
  
  /**
   * Returns the current endpoint sequence.
   */
  public final long getEndpointSequence()
  {
    return _endpointSequence;
  }
  
  /**
   * Receive at the endpoint
   */
  public final long receiveEndpoint()
  {
    return _endpointSequence++;
  }
  
  /**
   * Returns the current client sequence.
   */
  public final long getClientSequence()
  {
    return _clientSequence;
  }
  
  /**
   * Receive at the client
   */
  public final void receiveClient()
  {
    _clientSequence++;
  }
  
  /**
   * Returns the current credit available from the endpoint's perspective.
   */
  public final long getCreditAvailable()
  {
    return _creditEnd - _endpointSequence;
  }
  
  /**
   * Returns the current queue size, received but unprocessed
   */
  public final long getQueueSize()
  {
    return _endpointSequence - _clientSequence;
  }
  
  /**
   * Returns the credit to fill the prefetch queue
   */
  public final int getCredit()
  {
    return (int) (_prefetch - getQueueSize());
  }
  
  /**
   * Heuristic for sending the flow update. 
   */
  public final boolean isFlowRequired()
  {
    long clientQueueAvailable = getCredit();
    
    if (clientQueueAvailable == 0) {
      return false;
    }
    
    long creditAvailable = getCreditAvailable();
    
    if (creditAvailable <= _creditLowLimit) {
      return true;
    }
    else if (creditAvailable <= _creditHighLimit && ! _isHighUpdate) {
      _isHighUpdate = true;
      return true;
    }
    else {
      return false;
    }
  }
  
  public final void updateCredit(int credit)
  {
    long creditEnd = _endpointSequence + credit;
    
    _creditEnd = creditEnd;
    
    _isHighUpdate = false;
  }
}

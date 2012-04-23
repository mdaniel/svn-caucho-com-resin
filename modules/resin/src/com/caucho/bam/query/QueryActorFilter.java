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

package com.caucho.bam.query;


import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.broker.Broker;

/**
 * An ActorStream filter that intercepts query results and passes them to
 * the QueryManager to be matched with pending queries.
 */
public class QueryActorFilter implements Actor {
  private final Actor _next;
  private final QueryManager _queryManager;
  
  public QueryActorFilter(Actor next, 
                          QueryManager queryManager)
  {
    _next = next;
    _queryManager = queryManager;
  }
  
  @Override
  public String getAddress()
  {
    return _next.getAddress();
  }
  
  @Override
  public boolean isClosed()
  {
    return _next.isClosed();
  }
  
  @Override
  public Broker getBroker()
  {
    return _next.getBroker();
  }
  
  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    _next.message(to, from, payload);
  }
  
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    _next.messageError(to, from, payload, error);
  }

  @Override
  public void query(long id,
                    String to,
                    String from,
                    Serializable payload)
  {
    _next.query(id, to, from, payload);
  }

  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (! _queryManager.onQueryResult(id, to, from, payload)) {
      _next.queryResult(id, to, from, payload);
    }
  }

  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    if (! _queryManager.onQueryError(id, to, from, payload, error)) {
      _next.queryError(id, to, from, payload, error);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _next + "]";
  }
}

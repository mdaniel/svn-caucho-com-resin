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
 * @author Emil Ong
 */

package com.caucho.bam.proxy;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.Message;
import com.caucho.bam.MessageError;
import com.caucho.bam.Query;
import com.caucho.bam.QueryError;
import com.caucho.bam.QueryResult;
import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.actor.SkeletonActorFilter;
import com.caucho.bam.actor.SkeletonInvocationException;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.stream.MessageStream;
import com.caucho.util.L10N;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
public class ProxyActor<T> implements Actor
{
  private String _address;
  private T _bean;
  private Broker _broker;
  private ProxySkeleton<T> _skeleton;
  
  public ProxyActor(T bean,
                    String address,
                    Broker broker)
  {
    _address = address;
    _bean = bean;
    _broker = broker;
    _skeleton = ProxySkeleton.getSkeleton((Class<T>) bean.getClass());
  }

  @Override
  public String getAddress()
  {
    return _address;
  }

  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.stream.MessageStream#message(java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void message(String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.stream.MessageStream#messageError(java.lang.String, java.lang.String, java.io.Serializable, com.caucho.bam.BamError)
   */
  @Override
  public void messageError(String to, String from, Serializable payload,
                           BamError error)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.stream.MessageStream#query(long, java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.stream.MessageStream#queryResult(long, java.lang.String, java.lang.String, java.io.Serializable)
   */
  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see com.caucho.bam.stream.MessageStream#queryError(long, java.lang.String, java.lang.String, java.io.Serializable, com.caucho.bam.BamError)
   */
  @Override
  public void queryError(long id, String to, String from, Serializable payload,
                         BamError error)
  {
    // TODO Auto-generated method stub
    
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "," + _bean + "]";
  }
}

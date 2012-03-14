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

import com.caucho.message.MessagePropertiesFactory;


/**
 * message factory
 */
public class AbstractMessageFactory<T> 
  extends AbstractQueueSender<T>
  implements MessagePropertiesFactory<T>
{
  private boolean _isDurable;
  private int _priority = -1;
  private long _ttl = -1;
  private boolean _isFirstAcquirer = true;

  public void setDurable(boolean isDurable)
  {
    _isDurable = isDurable;
  }
  
  @Override
  public boolean isDurable()
  {
    return _isDurable;
  }

  @Override
  public int getPriority()
  {
    return _priority;
  }
  
  @Override
  public void setPriority(int priority)
  {
    _priority = priority;
  }

  @Override
  public long getTimeToLive()
  {
    return _ttl;
  }
  
  @Override
  public void setTimeToLive(long ttl)
  {
    _ttl = ttl;
  }

  @Override
  public boolean isFirstAcquirer()
  {
    return _isFirstAcquirer;
  }

  @Override
  public void setFirstAcquirer(boolean isFirst)
  {
    _isFirstAcquirer = isFirst;
  }

  @Override
  protected boolean offerMicros(MessagePropertiesFactory<T> factory, 
                                T value,
                                long timeoutMicros)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void close()
  {
  }
}

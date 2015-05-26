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

package com.caucho.nautilus.common;

import com.caucho.nautilus.MessageSettleListener;
import com.caucho.nautilus.MessagePropertiesFactory;
import com.caucho.nautilus.SenderQueue;
import com.caucho.nautilus.SettleMode;

/**
 * local connection to the message store
 */
abstract public class SenderQueueBase<T>
  extends SenderQueueAbstract<T>
  implements SenderQueue<T>
{
  private final SettleMode _settleMode;
  private final Settler _settler;
  
  protected SenderQueueBase(SettleMode settleMode,
                                 Settler settler)
  {
    _settleMode = settleMode;
    _settler = settler;
  }
  
  @Override
  public final SettleMode getSettleMode()
  {
    return _settleMode;
  }
  
  @Override
  public final MessageSettleListener getSettleListener()
  {
    // return _settleListener;
    return null;
  }
  
  /*
  @Override
  public MessagePropertiesFactory<T> createMessageFactory()
  {
    return new SenderMessageFactory<T>(this);
  }
  */
  
  @Override
  public int getUnsettledCount()
  {
    return 0;
  }

  /**
   * Offers a value to the queue.
   */
  abstract protected boolean offerMicros(MessagePropertiesFactory<T> factory,
                                         T value,
                                         long timeoutMicros);
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

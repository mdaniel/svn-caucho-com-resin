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
import com.caucho.message.MessageSender;
import com.caucho.message.MessageSenderFactory;
import com.caucho.message.MessageSettleListener;
import com.caucho.message.SettleMode;

/**
 * local connection to the message store
 */
abstract public class AbstractMessageSender<T>
  extends AbstractQueueSender<T>
  implements MessageSender<T>
{
  private final SettleMode _settleMode;
  private final MessageSettleListener _settleListener;
  
  protected AbstractMessageSender(MessageSenderFactory factory)
  {
    _settleMode = factory.getSettleMode();
    _settleListener = factory.getSettleListener();
  }
  
  @Override
  public final SettleMode getSettleMode()
  {
    return _settleMode;
  }
  
  public final MessageSettleListener getSettleListener()
  {
    return _settleListener;
  }
  
  @Override
  public MessagePropertiesFactory<T> createMessageFactory()
  {
    return new SenderMessageFactory<T>(this);
  }
  
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

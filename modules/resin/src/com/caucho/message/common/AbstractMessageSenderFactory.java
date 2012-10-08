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

import com.caucho.message.MessageEncoder;
import com.caucho.message.MessageSenderFactory;
import com.caucho.message.MessageSettleListener;
import com.caucho.message.SettleMode;
import com.caucho.message.encode.StringEncoder;
import com.caucho.util.L10N;

/**
 * local connection to the message store
 */
abstract public class AbstractMessageSenderFactory implements MessageSenderFactory {
  private static final L10N L = new L10N(AbstractMessageSenderFactory.class);
  
  private String _address;
  private SettleMode _settleMode = SettleMode.ALWAYS;
  private MessageSettleListener _settleListener;
  private MessageEncoder<?> _encoder = StringEncoder.ENCODER;

  @Override
  public AbstractMessageSenderFactory setAddress(String address)
  {
    _address = address;
    
    return this;
  }

  @Override
  public String getAddress()
  {
    return _address;
  }

  @Override
  public AbstractMessageSenderFactory setSettleMode(SettleMode settleMode)
  {
    _settleMode = settleMode;
    
    return this;
  }

  @Override
  public SettleMode getSettleMode()
  {
    return _settleMode;
  }

  @Override
  public AbstractMessageSenderFactory setSettleListener(MessageSettleListener listener)
  {
    _settleListener = listener;
    
    return this;
  }

  @Override
  public MessageSettleListener getSettleListener()
  {
    return _settleListener;
  }
  
  @Override
  public AbstractMessageSenderFactory setMessageEncoder(MessageEncoder<?> encoder)
  {
    _encoder = encoder;
    
    return this;
  }
  
  @Override
  public MessageEncoder<?> getMessageEncoder()
  {
    return _encoder;
  }
}

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

package com.caucho.v5.nautilus.common;

import com.caucho.v5.nautilus.EncoderMessage;
import com.caucho.v5.nautilus.MessageSettleListener;
import com.caucho.v5.nautilus.SenderQueueFactory;
import com.caucho.v5.nautilus.SettleMode;
import com.caucho.v5.nautilus.encode.StringEncoder;
import com.caucho.v5.util.L10N;

/**
 * local connection to the message store
 */
abstract public class SenderQueueFactoryBase implements SenderQueueFactory {
  private static final L10N L = new L10N(SenderQueueFactoryBase.class);
  
  private String _address;
  private SettleMode _settleMode = SettleMode.ALWAYS;
  private MessageSettleListener _settleListener;
  private EncoderMessage<?> _encoder = StringEncoder.ENCODER;

  @Override
  public SenderQueueFactoryBase setAddress(String address)
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
  public SenderQueueFactoryBase setSettleMode(SettleMode settleMode)
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
  public SenderQueueFactoryBase setSettleListener(MessageSettleListener listener)
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
  public SenderQueueFactoryBase setMessageEncoder(EncoderMessage<?> encoder)
  {
    _encoder = encoder;
    
    return this;
  }
  
  @Override
  public EncoderMessage<?> getMessageEncoder()
  {
    return _encoder;
  }
}

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

package com.caucho.v5.nautilus.protocol;

import com.caucho.v5.nautilus.DecoderMessage;
import com.caucho.v5.nautilus.ReceiverBuilder;
import com.caucho.v5.nautilus.ReceiverListener;
import com.caucho.v5.nautilus.ReceiverQueue;
import com.caucho.v5.nautilus.SettleMode;
import com.caucho.v5.nautilus.common.ReceiverBuilderBase;

/**
 * factory to create local receivers.
 */
class ReceiverFactoryNautilus extends ReceiverBuilderBase {
  private final ConnectionClientNautilus _conn;

  public ReceiverFactoryNautilus(ConnectionClientNautilus conn)
  {
    _conn = conn;
  }

  @Override
  public ReceiverFactoryNautilus address(String address)
  {
    super.address(address);
    
    return this;
  }

  @Override
  public ReceiverFactoryNautilus setSettleMode(SettleMode settleMode)
  {
    super.setSettleMode(settleMode);
    
    return this;
  }

  @Override
  public ReceiverFactoryNautilus prefetch(int prefetch)
  {
    super.prefetch(prefetch);

    return this;
  }
  
  ConnectionClientNautilus getConnection()
  {
    return _conn;
  }

  @Override
  public ReceiverQueue<?> build()
  {
    ReceiverClientNautilus<?> receiver
      = new ReceiverClientNautilus(this, _conn);
    
    receiver.onBuild();
    
    return receiver;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageReceiverFactory#setListener(com.caucho.message.MessageConsumer)
   */
  @Override
  public ReceiverBuilder setListener(ReceiverListener listener)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.message.MessageReceiverFactory#setMessageDecoder(com.caucho.message.MessageDecoder)
   */
  @Override
  public ReceiverBuilder setMessageDecoder(DecoderMessage decoder)
  {
    // TODO Auto-generated method stub
    return null;
  }
}

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

package com.caucho.v5.nautilus;

import java.util.function.Supplier;


/**
 * receiver configuration
 */
public interface ReceiverConfig<M>
{
  ReceiverMode getDistributionMode();
  
  int getPrefetch();
  
  SettleMode getSettleMode();
  
  SettleTime getSettleTime();
  
  Supplier<DecoderMessage<M>> getMessageDecoderSupplier();

  /*
  public static class Builder<M>
  {
    private int _prefetch = -1;
    private SettleMode _settleMode = SettleMode.ALWAYS;
    private SettleTime _settleTime= SettleTime.QUEUE_REMOVE;
    private DistributionMode _distributionMode = DistributionMode.MOVE;
    private Supplier<DecoderMessage<M>> _decoderSupplier;
    
    public static <M> Builder<M> create()
    {
      return new Builder<M>();
    }
    
    public Builder<M> prefetch(int prefetch)
    {
      _prefetch = prefetch;
      
      return this;
    }
    
    public Builder<M> subscribe()
    {
      _distributionMode = DistributionMode.COPY;
      
      return this;
    }
    
    public Builder<M> consume()
    {
      _distributionMode = DistributionMode.MOVE;
      
      return this;
    }
    
    public ReceiverConfig<M> build()
    {
      return new ReceiverConfigImpl<M>(_prefetch,
                                             _distributionMode,
                                             _settleMode,
                                             _settleTime,
                                             _decoderSupplier);
    }
  }
  */
}

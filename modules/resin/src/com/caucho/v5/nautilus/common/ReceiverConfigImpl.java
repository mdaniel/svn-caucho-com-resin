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

import java.util.function.Supplier;

import com.caucho.v5.nautilus.DecoderMessage;
import com.caucho.v5.nautilus.ReceiverConfig;
import com.caucho.v5.nautilus.ReceiverMode;
import com.caucho.v5.nautilus.SettleMode;
import com.caucho.v5.nautilus.SettleTime;


/**
 * receiver configuration
 */
class ReceiverConfigImpl<M> implements ReceiverConfig<M>
{
  private final ReceiverMode _distributionMode;
  private final int _prefetch;
  private final SettleMode _settleMode;
  private final SettleTime _settleTime;
  private final Supplier<DecoderMessage<M>> _decoderSupplier;
  
  public ReceiverConfigImpl(int prefetch,
                                   ReceiverMode distributionMode,
                                   SettleMode settleMode,
                                   SettleTime settleTime,
                                   Supplier<DecoderMessage<M>> decoderSupplier)
  {
    _prefetch = prefetch;
    _distributionMode = distributionMode;
    _settleMode = settleMode;
    _settleTime = settleTime;
    _decoderSupplier = decoderSupplier;
  }
  
  public ReceiverConfigImpl(ReceiverBuilderImpl<M> builder)
  {
    _prefetch = builder.getPrefetch();
    _distributionMode = builder.getReceiverMode();
    _settleMode = builder.getSettleMode();
    _settleTime = builder.getSettleTime();
    _decoderSupplier = null; // builder.getDecoderSupplier();
  }

  @Override
  public ReceiverMode getDistributionMode()
  {
    return _distributionMode;
  }
  
  @Override
  public int getPrefetch()
  {
    return _prefetch;
  }
  
  @Override
  public SettleMode getSettleMode()
  {
    return _settleMode;
  }
  
  @Override
  public SettleTime getSettleTime()
  {
    return _settleTime;
  }
  
  @Override
  public Supplier<DecoderMessage<M>> getMessageDecoderSupplier()
  {
    return _decoderSupplier;
  }
}

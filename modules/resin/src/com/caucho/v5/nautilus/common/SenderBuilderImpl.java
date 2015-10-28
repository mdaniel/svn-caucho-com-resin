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

import java.util.Objects;

import com.caucho.v5.nautilus.EncoderMessage;
import com.caucho.v5.nautilus.ReceiverController;
import com.caucho.v5.nautilus.ReceiverListener;
import com.caucho.v5.nautilus.ReceiverQueue;
import com.caucho.v5.nautilus.SenderBuilder;
import com.caucho.v5.nautilus.SenderQueue;

/**
 * local connection to the message store
 */
public class SenderBuilderImpl<M>
  implements SenderBuilder<M>
{
  private String _address;
  private EncoderMessage<?> _encoder = null; //NautilusEncoder.ENCODER;
  
  private final ContainerProviderImpl _container;

  public SenderBuilderImpl(ContainerProviderImpl container)
  {
    _container = container;
    Objects.requireNonNull(_container);
  }

  @Override
  public SenderBuilder<M> address(String address)
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
  public SenderQueue<M> build()
  {
    return getContainer().sender(getAddress(), createConfig());
  }

  private ContainerProviderImpl getContainer()
  {
    return _container;
  }

  private SenderConfigImpl<M> createConfig()
  {
    return new SenderConfigImpl<M>(this);
  }

  @Override 
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "]";
  }
}

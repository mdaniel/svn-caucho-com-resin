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

package com.caucho.bam.stream;

import com.caucho.bam.broker.Broker;


/**
 * NullActorStream always ignores messages and returns errors for RPC calls.
 */
public class NullMessageStream extends AbstractMessageStream
{
  private String _address;
  private Broker _broker;

  protected NullMessageStream()
  {
  }
  
  public NullMessageStream(String address, Broker broker)
  {
    if (broker == null)
      throw new IllegalArgumentException();
    
    _address = address;
    _broker = broker;
  }

  /**
   * Returns the address at the end of the stream.
   */
  @Override
  public String getAddress()
  {
    return _address;
  }
  
  protected void setAddress(String address)
  {
    _address = address;
  }

  /**
   * The stream to the link.
   */
  @Override
  public Broker getBroker()
  {
    return _broker;
  }
  
  protected void setBroker(Broker broker)
  {
    _broker = broker;
  }
}

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

package com.caucho.amp.actor;

import com.caucho.amp.stream.AbstractAmpStream;
import com.caucho.amp.stream.AmpEncoder;
import com.caucho.amp.stream.AmpError;
import com.caucho.amp.stream.NullEncoder;

/**
 * Abstract stream for an actor.
 */
public class AbstractAmpActor extends AbstractAmpStream implements AmpActor
{
  @Override
  public AmpMethodRef getMethod(String methodName, AmpEncoder encoder)
  {
    return new AbstractMethodRef(this, methodName, encoder);
  }
  
  static class AbstractMethodRef implements AmpMethodRef {
    private AbstractAmpActor _actor;
    private String _methodName;
    private AmpEncoder _encoder;
    
    public AbstractMethodRef(AbstractAmpActor actor,
                             String methodName, 
                             AmpEncoder encoder)
    {
      _actor = actor;
      _methodName = methodName;
      _encoder = encoder;
    }

    @Override
    public void send(AmpActorRef from, Object... args)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void query(long id, AmpActorRef from, Object... args)
    {
      from.queryError(id, null, NullEncoder.ENCODER, new AmpError());
    }
  }
}

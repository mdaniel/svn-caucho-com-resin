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

package com.caucho.amqp.io;

import java.io.IOException;


/**
 * AMQP composite list writer
 */
abstract public class AmqpAbstractComposite extends AmqpAbstractPacket {
  abstract protected long getDescriptorCode();
  
  abstract protected int writeBody(AmqpWriter out)
    throws IOException;
  
  abstract protected void readBody(AmqpReader in, int count)
    throws IOException;

  @Override
  public final void write(AmqpWriter out)
    throws IOException
  {
    out.writeDescriptor(getDescriptorCode());
    
    int offset = out.startList();
    
    int count = writeBody(out);
    
    out.finishList(offset, count);
  }
  
  @Override
  public void read(AmqpReader in)
    throws IOException
  {
    long code = in.readDescriptor();
    
    readValue(in);
  }
  
  @Override
  public void readValue(AmqpReader in)
    throws IOException
  {
    int count = in.startList();
    
    readBody(in, count);
    
    in.endList();
  }
}

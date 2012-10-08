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

package com.caucho.message.encode;

import java.io.IOException;
import java.io.OutputStream;

import com.caucho.amqp.io.AmqpStreamWriter;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.marshal.AmqpMessageEncoder;
import com.caucho.message.MessagePropertiesFactory;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

/**
 * null encoder to ignore messages.
 */
public class AmqpEncoder<T> extends AbstractMessageEncoder<T>
{
  private AmqpMessageEncoder<T> _encoder;
  
  public AmqpEncoder(AmqpMessageEncoder<T> encoder)
  {
    _encoder = encoder;
  }

  @Override
  public void encode(OutputStream out,
                     T value)
    throws IOException
  {
    WriteStream os = new WriteStream();
    os.init(new VfsStream(null, out));
    AmqpStreamWriter sout = new AmqpStreamWriter(os);
    AmqpWriter aout = new AmqpWriter();
    aout.initBase(sout);
    
    MessagePropertiesFactory<T> factory = null;
    
    _encoder.encode(aout, factory, value);
    
    sout.flush();
    os.flush();
  }
}

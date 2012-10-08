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

package com.caucho.message.tourmaline;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.caucho.message.MessageException;
import com.caucho.message.MessagePropertiesFactory;
import com.caucho.util.IoUtil;

/**
 * Custom serialization for the cache
 */
class NautilusClientSenderEndpoint<T> extends AbstractNautilusEndpoint
{
  private static final Logger log
    = Logger.getLogger(NautilusClientSenderEndpoint.class.getName());
  
  private final NautilusClientSender<T> _sender;
  
  NautilusClientSenderEndpoint(NautilusClientSender<T> sender)
  {
    _sender = sender;
  }
  
  void sendPublish(String queue)
    throws IOException
  {
    OutputStream os = getContext().startBinaryMessage();
    
    try {
      os.write(NautilusCode.PUBLISH.ordinal());
      
      write(os, "name:");
      write(os, queue);
      write(os, "\n");
    } finally {
      IoUtil.close(os);
    }
  }
  
  void send(MessagePropertiesFactory<T> factory,
            T value,
            long timeoutMicros)
  {
    OutputStream wsOut = null;
    
    try {
      wsOut = getContext().startBinaryMessage();

      wsOut.write(NautilusCode.SEND.ordinal());

      _sender.getEncoder().encode(wsOut, value);
    } catch (IOException e) {
      throw new MessageException(e);
    } finally {
      IoUtil.close(wsOut);
    }
  }
}

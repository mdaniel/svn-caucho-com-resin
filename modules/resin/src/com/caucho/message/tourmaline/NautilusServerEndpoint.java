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
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.message.broker.MessageBroker;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Custom serialization for the cache
 */
public class NautilusServerEndpoint extends AbstractNautilusEndpoint
{
  private static final Logger log
    = Logger.getLogger(NautilusServerEndpoint.class.getName());
  
  private MessageBroker _broker;
  
  private NautilusPublish _publish;
  private NautilusReceive _receive;
  
  NautilusServerEndpoint(MessageBroker broker)
  {
    _broker = broker;
  }
  
  MessageBroker getBroker()
  {
    return _broker;
  }
  
  NautilusPublish getPublish()
  {
    return _publish;
  }
  
  @Override
  protected void publishStart(InputStream is)
    throws IOException
  {
    ReadStream in = Vfs.openRead(is);
    
    NautilusPublish publish = new NautilusPublish(this);
    
    String line;
    
    while ((line = in.readLine()) != null) {
      int p = line.indexOf(':');
      
      if (p < 0)
        continue;
      
      String key = line.substring(0, p).trim();
      String value = line.substring(p + 1).trim();
      
      publish.add(key, value);
    }
    
    publishStart(publish);
  }
  
  private void publishStart(NautilusPublish publish)
  {
    _publish = publish;
    
    publish.init();
    
    if (log.isLoggable(Level.FINE))
      log.fine(this + " attach-sender " + publish);
  }
  
  @Override
  protected void receiveStart(InputStream is)
    throws IOException
  {
    ReadStream in = Vfs.openRead(is);
    
    NautilusReceive receive = new NautilusReceive(this);
    
    String line;
    
    while ((line = in.readLine()) != null) {
      int p = line.indexOf(':');
      
      if (p < 0)
        continue;
      
      String key = line.substring(0, p).trim();
      String value = line.substring(p + 1).trim();
      
      receive.add(key, value);
    }
    
    receiveStart(receive);
  }
  
  private void receiveStart(NautilusReceive receive)
  {
    _receive = receive;
    
    receive.init();
    
    if (log.isLoggable(Level.FINE))
      log.fine(this + " attach-receiver " + receive);
  }

  @Override
  protected void onSend(InputStream is)
    throws IOException
  {
    _publish.publish(is);
  }
  
  @Override
  protected void onFlow(int credit, long sequence)
  {
    _receive.flow(sequence, credit);
  }
  
  @Override
  protected void onClose()
  {
    NautilusPublish publish = _publish;
    _publish = null;
    
    if (publish != null)
      publish.close();
    
    NautilusReceive receiver = _receive;
    _receive = null;
    
    if (receiver != null)
      receiver.close();
  }
}

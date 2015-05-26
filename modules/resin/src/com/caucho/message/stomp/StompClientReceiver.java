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

package com.caucho.message.stomp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

/**
 * Custom serialization for the cache
 */
class StompClientReceiver implements Runnable
{
  private static final L10N L = new L10N(StompClientReceiver.class);
  private static final Logger log
    = Logger.getLogger(StompClientReceiver.class.getName());

  private StompClient _client;
  private ReadStream _is;
  private volatile boolean _isClosed;
  
  private boolean _isConnected;
  
  StompClientReceiver(StompClient client, ReadStream is)
  {
    _client = client;
    _is = is;
  }
  
  void close()
  {
    _isClosed = true;
  }
  
  @Override
  public void run()
  {
    try {
      while (! _isClosed && readMessage()) {
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  boolean isConnected()
  {
    return _isConnected && ! _isClosed;
  }
  
  boolean readMessage()
    throws IOException
  {
    String cmd = _is.readLine();

    if (_isClosed)
      return false;
    
    if (cmd == null)
      throw new IOException("EOF:");
    
    HashMap<String,String> headers = new HashMap<String,String>();
    
    String line;
    while ((line = _is.readLine()) != null && ! "".equals(line)) {
      int p = line.indexOf(':');
      
      if (p < 0)
        break;
      
      String key = line.substring(0, p);
      String value = line.substring(p + 1);
      
      headers.put(key, value);
    }
    
    StringBuffer sb = new StringBuffer();
    
    int ch;
    
    while ((ch = _is.read()) > 0) {
      sb.append((char) ch);
    }
    
    String body = sb.toString();
    
    if (ch != 0) {
      throw new IOException("Unexpected body");
    }
    
    if ("CONNECTED".equals(cmd)) {
      _isConnected = true;
      return true;
    }
    else if ("MESSAGE".equals(cmd)) {
      addMessage(headers, body);
      return true;
    }
    
    System.out.println("CMDR: " + cmd + " " + headers + "\n" + body);
    
    if (true)
      throw new IOException(L.l("unknown command: " + cmd));
    
    return true;
  }
  
  private void addMessage(HashMap<String,String> headers, String body)
  {
    String subscription = headers.get("subscription");
    
    _client.addMessage(subscription, headers, body);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _client + "]";
  }
}

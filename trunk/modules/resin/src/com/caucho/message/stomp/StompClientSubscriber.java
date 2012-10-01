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
public class StompClientSubscriber
{
  private static final L10N L = new L10N(StompClientSubscriber.class);
  private static final Logger log
    = Logger.getLogger(StompClientSubscriber.class.getName());

  private StompClient _client;
  private String _id;
  
  private ArrayList<Msg> _queue = new ArrayList<Msg>();
  private boolean _isClosed;
  
  StompClientSubscriber(StompClient client, String id)
  {
    _client = client;
    _id = id;
  }
  
  public String receive()
  {
    synchronized (_queue) {
      if (_queue.size() == 0) {
        try {
          _queue.wait(1000);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
        
      if (_queue.size() > 0) {
        Msg msg = _queue.remove(0);
          
        return msg.getBody();
      }
    }
    
    return null;
  }
  
  void close()
  {
    _isClosed = true;
    
    synchronized (_queue) {
      _queue.notifyAll();
    }
  }
  
  void addMessage(HashMap<String,String> headers, String body)
  {
    String destination = headers.get("destination");
    long id = Long.parseLong(headers.get("message-id"));
    
    Msg msg = new Msg(id, destination, body);
    
    synchronized (_queue) {
      _queue.add(msg);
      _queue.notifyAll();
    }
  }
  
  static class Msg {
    private long _id;
    private String _destination;
    
    private String _body;
    
    Msg(long id, String destination, String body)
    {
      _id = id;
      _destination = destination;
      
      _body = body;
    }
    
    long getId()
    {
      return _id;
    }
    
    String getBody()
    {
      return _body;
    }
  }
}

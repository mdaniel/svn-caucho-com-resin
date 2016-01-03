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

package com.caucho.v5.message.stomp;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.io.IoUtil;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class StompClient
{
  private static final Logger log
    = Logger.getLogger(StompClient.class.getName());
  
  private String _address;
  private int _port;
  
  private ReadStream _is;
  private WriteStream _os;
  
  private Socket _s;
  
  private StompClientReceiver _receiver;
  
  private ConcurrentHashMap<String,StompClientSubscriber> _subscriptions
    = new ConcurrentHashMap<String,StompClientSubscriber>();
  
  private int _subId;
  
  public StompClient(String address, int port)
  {
    _address = address;
    _port = port;
  }
  
  public void connect(String user, String password)
    throws IOException
  {
    _s = new Socket(_address, _port);
    
    _is = Vfs.openRead(_s.getInputStream());
    _os = Vfs.openWrite(_s.getOutputStream());
    
    _os.print("CONNECT\n");
    _os.print("user:" + user + "\n");
    _os.print("password:" + password + "\n");
    _os.print("\n\0");
    _os.flush();
    
    _receiver = new StompClientReceiver(this, _is);
    
    if (! _receiver.readMessage()) {
      close();
      throw new IOException("can't connect");
    }
    
    if (! _receiver.isConnected()) {
      close();
      throw new IOException("can't connect2");
    }
    
    ThreadPool.getCurrent().schedule(_receiver);
    // check
  }
  
  public void send(String dest, String msg)
    throws IOException
  {
    _os.print("SEND\n");
    _os.print("destination:" + dest + "\n");
    _os.print("content-length:" + msg.length() + "\n");
    _os.print("\n");
    _os.print(msg);
    _os.print("\0");
    _os.flush();
  }
  
  public StompClientSubscriber createSubscription(String dest)
    throws IOException
  {
    String sid = "t" + _subId++;
    
    StompClientSubscriber sub = new StompClientSubscriber(this, sid);
    
    _subscriptions.put(sid, sub);
    
    _os.print("SUBSCRIBE\n");
    _os.print("destination:" + dest + "\n");
    _os.print("id:" + sid + "\n");
    _os.print("\n");
    _os.print("\0");
    _os.flush();
    
    // return receiveMessage();
    
    return sub;
  }
  
  void addMessage(String subId, HashMap<String,String> headers, String body)
  {
    StompClientSubscriber sub = _subscriptions.get(subId);
    
    if (sub != null) {
      sub.addMessage(headers, body);
    }
    else {
      System.out.println("UNKNOWN_SUB: " + subId);
    }
  }

  public void close()
  {
    try {
      disconnect();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      closeSockets();
    }
  }
  
  private void disconnect()
    throws IOException
  {
    WriteStream out = _os;
    
    if (out == null)
      return;
    
    out.println("DISCONNECT\n\n\0");
    out.flush();
  }

  private void closeSockets()
  {
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;
    
    Socket s = _s;
    _s = null;
    
    StompClientReceiver receiver = _receiver;
    _receiver = null;
    
    if (receiver != null) {
      receiver.close();
    }
    
    for (StompClientSubscriber sub : _subscriptions.values()) {
      sub.close();
    }
    
    _subscriptions.clear();
    
    IoUtil.close(is);
    IoUtil.close(os);
    
    try {
      if (s != null)
        s.close();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}

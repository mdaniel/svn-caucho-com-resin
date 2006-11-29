/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.burlap;

import com.caucho.ejb.burlap.BurlapReader;
import com.caucho.ejb.burlap.BurlapWriter;
import com.caucho.jms.session.MessageAvailableListener;
import com.caucho.log.Log;
import com.caucho.services.message.MessageSender;
import com.caucho.services.message.MessageServiceException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * The Burlap queue is a write-only queue.
 */
public class BurlapQueue extends com.caucho.jms.AbstractQueue
  implements MessageSender {
  static final Logger log = Log.open(BurlapQueue.class);
  static final L10N L = new L10N(BurlapQueue.class);

  private String _queueName;
  private String _url;
  private Path _path;

  public BurlapQueue()
  {
  }

  /**
   * Returns the queue's name.
   */
  public String getQueueName()
  {
    return _queueName;
  }

  /**
   * Sets the queue's name.
   */
  public void setQueueName(String name)
  {
    _queueName = name;
  }

  public void setURL(String url)
  {
    _url = url;
  }

  public String getURL()
  {
    return _url;
  }

  public void setPath(Path path)
  {
    _path = path;
  }

  public Path getPath()
  {
    if (_path == null)
      _path = Vfs.lookup(_url);
    
    return _path;
  }
  
  /**
   * Adds a message available listener.
   */
  public void addListener(MessageAvailableListener listener)
  {
    throw new UnsupportedOperationException();
  }

  public void send(Message message)
    throws JMSException
  {
    try {
      HashMap<String,Object> headers = new HashMap<String,Object>();

      Enumeration names = message.getPropertyNames();
      while (names != null && names.hasMoreElements()) {
        String name = (String) names.nextElement();

        Object value = message.getObjectProperty(name);

        headers.put(name, value);
      }

      if (message instanceof TextMessage)
        send(headers, ((TextMessage) message).getText());
      else if (message instanceof ObjectMessage)
        send(headers, ((ObjectMessage) message).getObject());
      else
        send(headers, message);
    } catch (Exception e) {
      throw new JMSException(String.valueOf(e));
    }
  }

  public void send(HashMap headers, Object data)
    throws MessageServiceException
  {
    ReadStream is = null;
    WriteStream os = null;
    
    try {
      ReadWritePair pair = getPath().openReadWrite();
      is = pair.getReadStream();
      os = pair.getWriteStream();

      BurlapWriter out = new BurlapWriter(os);
      BurlapReader in = new BurlapReader(is);

      out.startCall("send");

      out.writeObject(headers);
      out.writeObject(data);

      out.completeCall();

      os.flush();

      String status = (String) is.getAttribute("status");

      if (! "200".equals(status)) {
	CharBuffer errorMsg = new CharBuffer();
	int ch;
	
	while ((ch = is.readChar()) >= 0) {
	  errorMsg.append((char) ch);
	}

	throw new MessageServiceException(errorMsg.toString());
      }

      Object result = in.readReply(null);
    } catch (Throwable e) {
      throw new MessageServiceException(e);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
        }
      }
      
      if (is != null) {
	is.close();
      }
    }
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "BurlapQueue[" + _queueName + "]";
  }
}


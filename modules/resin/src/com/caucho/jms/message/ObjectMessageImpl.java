/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jms.message;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import javax.jms.ObjectMessage;
import javax.jms.JMSException;

import com.caucho.vfs.TempStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ContextLoaderObjectInputStream;

import com.caucho.jms.JMSExceptionWrapper;

/**
 * An object message.
 */
public class ObjectMessageImpl extends MessageImpl implements ObjectMessage  {
  private TempStream _tempStream;
  
  /**
   * Writes the object to the stream.
   */
  public void setObject(Serializable o)
    throws JMSException
  {
    checkBodyWriteable();
    
    _tempStream = new TempStream(null);
    
    try {
      WriteStream ws = new WriteStream(_tempStream);
      ObjectOutputStream oos = new ObjectOutputStream(ws);
      oos.writeObject(o);
      oos.close();
      ws.close();
    } catch (Exception e) {
      throw JMSExceptionWrapper.create(e);
    }
  }

  /**
   * Reads the object from the stream.
   */
  public Serializable getObject()
    throws JMSException
  {
    if (_tempStream == null)
      return null;
    
    try {
      ReadStream is = _tempStream.openRead(false);
      ObjectInputStream ois = new ContextLoaderObjectInputStream(is);
      Serializable object = (Serializable) ois.readObject();
      ois.close();
      is.close();

      return object;
    } catch (Exception e) {
      e.printStackTrace();
      throw JMSExceptionWrapper.create(e);
    }
  }
  
  /**
   * Clears the body
   */
  public void clearBody()
    throws JMSException
  {
    super.clearBody();
    
    _tempStream = null;
  }

  public MessageImpl copy()
  {
    ObjectMessageImpl msg = new ObjectMessageImpl();

    copy(msg);
    
    msg._tempStream = _tempStream;

    return msg;
  }
}


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

package com.caucho.ejb.protocol;

import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.rmi.RemoteException;
/**
 * Serializer for transfers inside a single JVM.
 */
public class SelfSerializer {
  TempStream trs;

  ReadStream is;
  WriteStream os;
  
  ObjectInputStream ois;
  ObjectOutputStream oos;
  
  public static SelfSerializer allocate()
    throws RemoteException
  {
    SelfSerializer ser = new SelfSerializer();

    ser.clear();

    return ser;
  }

  /**
   * Clears the buffers for the serializer
   */
  public void clear()
    throws RemoteException
  {
    try {
      trs = new TempStream();
      os = new WriteStream(trs);
      oos = new ObjectOutputStream(os);
      is = null;
      ois = null;
    } catch (Exception e) {
      throw new RemoteExceptionWrapper(e);
    }
  }

  /**
   * Serializes the object to the temp stream.
   *
   * @param obj the object to serialize
   */
  public void write(Object obj)
    throws RemoteException
  {
    try {
      oos.writeObject(obj);
    } catch (Exception e) {
      throw new RemoteExceptionWrapper(e);
    }
  }

  /**
   * Reads an object from the serialized stream.
   */
  public Object read()
    throws RemoteException
  {
    try {
      if (is == null) {
        oos.flush();
        is = trs.openRead(true);
        ois = new LoaderObjectInputStream(is);
      }
      
      return ois.readObject();
    } catch (Exception e) {
      throw new RemoteExceptionWrapper(e);
    }
  }

  /**
   * Closes the streams.
   */
  public void close()
  {
    try {
      InputStream is = this.is;
      this.is = null;
      OutputStream os = this.os;
      this.os = null;
      if (is != null)
        is.close();
      if (os != null)
        os.close();
    } catch (IOException e) {
    }
  }
  
  /**
   * Extension class so the classes will be loaded by the proper
   * class loader.
   */
  static class LoaderObjectInputStream extends ObjectInputStream {
    ClassLoader loader;
    
    LoaderObjectInputStream(InputStream is)
      throws IOException, StreamCorruptedException
    {
      super(is);

      loader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Finds the specified class using the current class loader.
     */
    protected Class resolveClass(ObjectStreamClass v)
      throws IOException, ClassNotFoundException
    {
      return CauchoSystem.loadClass(v.getName(), false, loader);
    }
  }
}

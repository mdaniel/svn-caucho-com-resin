/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.hessian;

import com.caucho.config.ConfigException;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.ejb.protocol.AbstractHomeHandle;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.util.CharBuffer;

import javax.ejb.EJBException;
import java.lang.reflect.Field;

/**
 * Encodes and decodes handles.
 */
public class HessianHandleEncoder extends HandleEncoder {
  private static char []_serverEncode64;
  private static int []_serverHash64;
  private static char []_serverEncodeH2;

  private Class _primaryKeyClass;
  private Field []_fields;
  private int []_fieldTypes;

  public HessianHandleEncoder(AbstractServer server, String serverId,
                             Class primaryKeyClass)
    throws ConfigException
  {
    super(serverId);
    
    _primaryKeyClass = primaryKeyClass;

    if (_primaryKeyClass == null ||
        _primaryKeyClass.isPrimitive() ||
        _primaryKeyClass.getName().startsWith("java.lang."))
      return;

    _fields = _primaryKeyClass.getFields();
    _fieldTypes = new int[_fields.length];

    for (int i = 0; i < _fields.length; i++)
      _fields[i].setAccessible(true);

    setServer(server);
  }
  
  /**
   * Creates a home handle given the server id.
   */
  public AbstractHomeHandle createHomeHandle()
  {
    try {
      return new HessianHomeHandle(getServer().getEJBHome(), getServerId());
    } catch (Throwable e) {
      return new HessianHomeHandle(getServerId());
    }
  }

  /**
   * Creates a handle given the server id and the object id.
   */
  public AbstractHandle createHandle(String objectId)
  {
    HessianHandle handle = new HessianHandle(getURL(objectId), objectId);

    return handle;
  }

  /**
   * Creates a random string key which hashes to this server.
   */
  public String createRandomStringKey()
  {
    String originalKey = super.createRandomStringKey();

    int srun = 0;
    int tail = calculateHash(originalKey) * 65521;

    CharBuffer cb = CharBuffer.allocate();
    cb.append(originalKey);

    int d1 = (64 + srun - tail) & 0x3f;
    cb.append(_serverEncode64[d1]);

    return cb.close();
  }

  /**
   * Returns the hash of a string for load-balancing.
   */
  private int calculateHash(String key)
  {
    int hash = 137;
    int len = key.length();
    for (int i = 0; i < len; i++) {
      char ch = key.charAt(i);
      hash = 65521 * hash + _serverHash64[ch & 0xff];
    }

    return hash;
  }

  /**
   * Converts a string object id to the correct key.
   */
  public Object objectIdToKey(Object objectKey)
  {
    String objectId = (String) objectKey;

    if (_primaryKeyClass == null)
      return objectId;
    else if (_primaryKeyClass.equals(String.class))
      return objectId;
    else if (_primaryKeyClass.equals(Integer.class) ||
             _primaryKeyClass.equals(int.class))
      return new Integer(objectId);
    else if (_primaryKeyClass.equals(Long.class) ||
             _primaryKeyClass.equals(long.class))
      return new Long(objectId);
    else if (_primaryKeyClass.equals(Float.class) ||
             _primaryKeyClass.equals(float.class))
      return new Float(objectId);
    else if (_primaryKeyClass.equals(Double.class) ||
             _primaryKeyClass.equals(double.class))
      return new Double(objectId);
    else if (_fields != null) {
      Object obj;

      try {
        obj = _primaryKeyClass.newInstance();

        int length = objectId.length();
        int j = 0;
        
        CharBuffer cb = new CharBuffer();
        for (int i = 0; i < _fields.length; i++) {
          cb.clear();
          for (; j < length && objectId.charAt(j) != ','; j++)
            cb.append(objectId.charAt(j));

          j++;

          String field = cb.toString();
          Class fieldClass = _fields[i].getType();
        
          if (fieldClass.equals(String.class))
            _fields[i].set(obj, field);
          else if (fieldClass.equals(int.class))
            _fields[i].setInt(obj, Integer.parseInt(field));
          else if (fieldClass.equals(Integer.class))
            _fields[i].set(obj, new Integer(field));
          else if (fieldClass.equals(long.class))
            _fields[i].setLong(obj, Long.parseLong(field));
          else if (fieldClass.equals(Long.class))
            _fields[i].set(obj, new Long(field));
          else if (fieldClass.equals(float.class))
            _fields[i].setFloat(obj, (float) Double.parseDouble(field));
          else if (fieldClass.equals(Float.class))
            _fields[i].set(obj, new Float(field));
          else if (fieldClass.equals(double.class))
            _fields[i].setDouble(obj, Double.parseDouble(field));
          else if (fieldClass.equals(Double.class))
            _fields[i].set(obj, new Double(field));
          else
            throw new RuntimeException();
        }

        j++;
      } catch (Exception e) {
        throw EJBExceptionWrapper.create(e);
      }
      
      return obj;
    }
    else
      throw new EJBException("bad primary key class");
  }

  /**
   * Initialize the server encoding hash.
   */
  static {
    _serverEncode64 = new char[64];
    for (int i = 0; i < 26; i++) {
      _serverEncode64[i] = (char) ('a' + i);
      _serverEncode64[i + 26] = (char) ('A' + i);
    }
    for (int i = 0; i < 10; i++)
      _serverEncode64[i + 52] = (char) ('0' + i);
    _serverEncode64[62] = '-';
    _serverEncode64[63] = '_';
    
    _serverHash64 = new int[256];
    
    for (int i = 0; i < 26; i++) {
      _serverHash64['a' + i] = i;
      _serverHash64['A' + i] = i + 26;
    }
    for (int i = 0; i < 10; i++)
      _serverHash64['0' + i] = 52 + i;
    _serverHash64['-'] = 62;
    _serverHash64['_'] = 63;

    int j = 64;
    for (int i = 0; i < 256; i++) {
      if (_serverHash64[i] == 0)
        _serverHash64[i] = j++;
    }
    
    _serverEncodeH2 = new char[64];
    j = 0;
    for (int i = 0; i < 64; i++) {
      _serverEncodeH2[j] = _serverEncode64[i];
      j = (j + 49) % 64;
    }
  }
}

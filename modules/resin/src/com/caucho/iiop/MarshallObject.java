/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.iiop;

import java.util.logging.Logger;

import java.lang.reflect.*;
import java.io.*;

import javax.rmi.CORBA.Util;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.ejb.*;
import com.caucho.ejb.protocol.AbstractHandle;
import com.caucho.log.Log;

public class MarshallObject {
  private static final Logger log = Log.open(MarshallObject.class);
  
  public final static int VOID = -1;
  
  public final static int BOOLEAN = 0;
  public final static int BYTE = 1;
  public final static int SHORT = 2;
  public final static int CHAR = 3;
  public final static int INT = 4;
  public final static int LONG = 5;
  public final static int FLOAT = 6;
  public final static int DOUBLE = 7;
  
  public final static int STRING = 8;
  
  public final static int BOOLEAN_ARRAY = 9;
  public final static int BYTE_ARRAY = 10;
  public final static int SHORT_ARRAY = 11;
  public final static int CHAR_ARRAY = 12;
  public final static int INT_ARRAY = 13;
  public final static int LONG_ARRAY = 14;
  public final static int FLOAT_ARRAY = 15;
  public final static int DOUBLE_ARRAY = 16;
  public final static int STRING_ARRAY = 17;

  public final static int OBJECT = 20;
  
  public final static int CORBA_OBJECT = 21;
  public final static int REMOTE = 22;
  public final static int EJB_HOME = 23;
  public final static int EJB_OBJECT = 24;
  
  public final static int OBJECT_ARRAY = 25;
  
  public final static int OBJECT_HELPER = 26;
  
  private int _code;
  private Class objClass;
  private MarshallObject subObj;
  private Method readHelper;
  private Method writeHelper;

  private MarshallObject(int code)
  {
    _code = code;
  }

  private MarshallObject(int code, Class cl)
  {
    _code = code;
    this.objClass = cl;
  }

  private MarshallObject(int code, Class cl,
                         Method readHelper, Method writeHelper)
  {
    _code = code;
    this.objClass = cl;
    this.readHelper = readHelper;
    this.writeHelper = writeHelper;
  }

  static MarshallObject create(Class cl, boolean isJava)
  {
    if (void.class.equals(cl))
      return new MarshallObject(VOID);
    else if (boolean.class.equals(cl) || Boolean.class.equals(cl))
      return new MarshallObject(BOOLEAN);
    else if (byte.class.equals(cl) || Byte.class.equals(cl))
      return new MarshallObject(BYTE);
    else if (short.class.equals(cl) || Short.class.equals(cl))
      return new MarshallObject(SHORT);
    else if (char.class.equals(cl) || Character.class.equals(cl))
      return new MarshallObject(CHAR);
    else if (int.class.equals(cl) || Integer.class.equals(cl))
      return new MarshallObject(INT);
    else if (long.class.equals(cl) || Long.class.equals(cl))
      return new MarshallObject(LONG);
    else if (float.class.equals(cl) || Float.class.equals(cl))
      return new MarshallObject(FLOAT);
    else if (double.class.equals(cl) || Double.class.equals(cl))
      return new MarshallObject(DOUBLE);
    else if (String.class.equals(cl))
      return new MarshallObject(STRING);
    else if (String[].class.equals(cl) && ! isJava)
      return new MarshallObject(STRING_ARRAY);
    else if (javax.ejb.EJBHome.class.isAssignableFrom(cl))
      return new MarshallObject(EJB_HOME, cl);
    else if (javax.ejb.EJBObject.class.isAssignableFrom(cl))
      return new MarshallObject(EJB_OBJECT, cl);
    else if (java.rmi.Remote.class.isAssignableFrom(cl))
      return new MarshallObject(REMOTE, cl);
    else if (org.omg.CORBA.Object.class.isAssignableFrom(cl))
      return new MarshallObject(CORBA_OBJECT, cl);
    else if (cl.isArray() && ! isJava) {
      Class compType = cl.getComponentType();
      MarshallObject subObj = MarshallObject.create(compType, isJava);
      MarshallObject obj = new MarshallObject(OBJECT_ARRAY, compType);
      obj.subObj = subObj;

      return obj;
    }
    else {
      Class helperClass = null;
      Method readHelper = null;
      Method writeHelper = null;
      try {
        helperClass = CauchoSystem.loadClass(cl.getName() + "Helper");

        readHelper = helperClass.getMethod("read", new Class[] {
          org.omg.CORBA.portable.InputStream.class
        });
        
        writeHelper = helperClass.getMethod("write", new Class[] {
          org.omg.CORBA.portable.OutputStream.class, cl
        });
      } catch (Exception e) {
      }

      if (readHelper != null)
        return new MarshallObject(OBJECT_HELPER, cl, readHelper, writeHelper);
      else
        return new MarshallObject(OBJECT, cl);
    }
  }

  public Object unmarshall(IiopReader reader)
    throws Exception
  {
    switch (_code) {
    case BOOLEAN:
      return new Boolean(reader.read_boolean());
    case BYTE:
      return new Byte(reader.read_octet());
    case SHORT:
      return new Short(reader.read_short());
    case CHAR:
      return new Character(reader.read_wchar());
    case INT:
      return new Integer(reader.read_long());
    case LONG:
      return new Long(reader.read_longlong());
    case FLOAT:
      return new Float(reader.read_float());
    case DOUBLE:
      return new Double(reader.read_double());
    case STRING:
      return (String) reader.read_value(String.class);
    case BOOLEAN_ARRAY:
    {
      boolean []array = new boolean[reader.read_sequence_length()];
      reader.read_boolean_array(array, 0, array.length);
      return array;
    }
    case BYTE_ARRAY:
    {
      byte []array = new byte[reader.read_sequence_length()];
      reader.read_octet_array(array, 0, array.length);
      return array;
    }
    case CHAR_ARRAY:
    {
      char []array = new char[reader.read_sequence_length()];
      reader.read_wchar_array(array, 0, array.length);
      return array;
    }
    case SHORT_ARRAY:
    {
      short []array = new short[reader.read_sequence_length()];
      reader.read_short_array(array, 0, array.length);
      return array;
    }
    case INT_ARRAY:
    {
      int []array = new int[reader.read_sequence_length()];
      reader.read_long_array(array, 0, array.length);
      return array;
    }
    case LONG_ARRAY:
    {
      long []array = new long[reader.read_sequence_length()];
      reader.read_longlong_array(array, 0, array.length);
      return array;
    }
    case FLOAT_ARRAY:
    {
      float []array = new float[reader.read_sequence_length()];
      reader.read_float_array(array, 0, array.length);
      return array;
    }
    case DOUBLE_ARRAY:
    {
      double []array = new double[reader.read_sequence_length()];
      reader.read_double_array(array, 0, array.length);
      return array;
    }
    /*
    case STRING_ARRAY:
    {
      String []array = new String[reader.read_sequence_length()];
      for (int i = 0; i < array.length; i++) {
        array[i] = reader.read_wstring();
	System.out.println(array[i]);
      }
      
      return array;
    }
    */
    
    case REMOTE:
      return reader.readObject(objClass);
    
    case CORBA_OBJECT:
      return reader.read_Object();
    
    case OBJECT_ARRAY:
    {
      int len = reader.read_sequence_length();
      Object []obj = (Object []) Array.newInstance(objClass, len);

      for (int i = 0; i < len; i++)
        obj[i] = subObj.unmarshall(reader);
      
      return obj;
    }
    
    case OBJECT_HELPER:
    {
      return readHelper.invoke(null, new Object[] { reader });
    }
      
    default:
      try {
	log.info("Class: " + objClass);
	
        return reader.read_value(objClass);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }
  }

  public void marshall(Object obj, IiopWriter writer)
    throws Exception
  {
    switch (_code) {
    case BOOLEAN:
      writer.write_boolean(((Boolean) obj).booleanValue());
      break;
    case BYTE:
      writer.write_octet(((Byte) obj).byteValue());
      break;
    case SHORT:
      writer.write_short(((Short) obj).shortValue());
      break;
    case INT:
      writer.write_long(((Integer) obj).intValue());
      break;
    case LONG:
      writer.write_longlong(((Long) obj).longValue());
      break;
    case FLOAT:
      writer.write_float(((Float) obj).floatValue());
      break;
    case DOUBLE:
      writer.write_double(((Double) obj).doubleValue());
      break;
    case CHAR:
      writer.write_wchar(((Character) obj).charValue());
      break;
    case STRING:
      writer.write_value((String) obj, String.class);
      break;
    case EJB_OBJECT:
    case REMOTE:
      if (obj instanceof AbstractEJBObject) {
        AbstractEJBObject absObj = (AbstractEJBObject) obj;


        AbstractServer server = absObj.__caucho_getServer();
	String local = absObj.__caucho_getId();

	String url = server.getEJBName() + "?" + local;
	String typeName = "RMI:" + objClass.getName() + ":0";

	System.out.println("TYPE: " + typeName);
	
	IOR ior = new IOR(typeName, writer.getHost(), writer.getPort(), url);
	//writer.write_boolean(true);
	writer.write_Object(new DummyObjectImpl(ior));
      }
      else {
	//writer.write_boolean(false);
	writer.write_value((Serializable) obj);
      }

      break;
      /*
      if (obj instanceof AbstractEJBObject) {
        AbstractEJBObject absObj = (AbstractEJBObject) obj;

        AbstractServer server = absObj.__caucho_getServer();
	String local = absObj.__caucho_getId();

	String url = server.getEJBName() + "?" + local;
	String typeName = "RMI:" + objClass.getName() + ":0";

	IOR ior = new IOR(typeName, writer.getHost(), writer.getPort(), url);
	writer.write_Object(new DummyObjectImpl(ior));
	System.out.println("REMOTE: " + writer.getHost() + ":" + writer.getPort());
      }
      else {
	Util.writeRemoteObject(writer, obj);
      }
      
      break;
      */
    case EJB_HOME:
	System.out.println("HOME: " + writer.getHost() + ":" + writer.getPort());
      Util.writeRemoteObject(writer, obj);
      break;
    case CORBA_OBJECT:
      writer.write_Object((org.omg.CORBA.Object) obj);
      break;
    case OBJECT_HELPER:
    {
      writeHelper.invoke(null, new Object[] { writer, obj });
      break;
    }
    default:
      writer.write_value((Serializable) obj);
      break;
    }
  }
}

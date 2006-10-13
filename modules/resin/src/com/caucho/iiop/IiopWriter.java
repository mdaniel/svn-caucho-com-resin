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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import java.io.Serializable;
import java.io.IOException;

import javax.rmi.CORBA.*;

import org.omg.CORBA.*;
import org.omg.CORBA.Object;

import org.omg.SendingContext.RunTime;

import com.caucho.util.IdentityIntMap;
import com.caucho.util.Alarm;

abstract public class IiopWriter extends org.omg.CORBA_2_3.portable.OutputStream {
  static protected final int VALUE_TAG = 0x7fffff00;
  static protected final int VALUE_HAS_CODESET = 0x1;
  static protected final int VALUE_NO_TYPE = 0x0;
  static protected final int VALUE_ONE_REP_ID = 0x2;
  static protected final int VALUE_MANY_REP_IDS = 0x6;
  
  protected MessageWriter _out;
  protected int _type;

  protected SystemException _nullSystemException;
  
  protected IiopReader _reader;

  protected String _host;
  protected int _port;

  private IdentityIntMap _refMap = new IdentityIntMap();
  
  private ValueHandler valueHandler = Util.createValueHandler();
  private RunTime runTime = valueHandler.getRunTimeCodeBase();

  /**
   * Initialize the writer with a new underlying stream.
   *
   * @param ws the underlying write stream.
   */
  public void init(MessageWriter out)
  {
    _out = out;

    _refMap.clear();
  }

  /**
   * Initialize the writer with a new underlying stream and a reader.
   *
   * @param ws the underlying write stream.
   * @param reader the reader
   */
  public void init(MessageWriter out, IiopReader reader)
  {
    init(out);
    
    _reader = reader;
  }

  /**
   * Set the default host.
   */
  public void setHost(String host)
  {
    _host = host;
  }

  /**
   * Get the default host.
   */
  public String getHost()
  {
    return _host;
  }

  /**
   * Set the default port.
   */
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Get the default port.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Writes the header for a request
   *
   * @param operation the method to call
   */
  public void startRequest(IOR ior, String operation)
    throws IOException
  {
    byte []bytes = ior.getOid();

    startRequest(bytes, 0, bytes.length, operation);
  }
  
  /**
   * Writes the header for a request
   *
   * @param operation the method to call
   */
  public void startRequest(byte []oid, int off, int len, String operation)
    throws IOException
  {
    startRequest(oid, off, len, operation, (int) Alarm.getCurrentTime());
  }
  
  /**
   * Writes the header for a request
   *
   * @param operation the method to call
   */
  abstract public void startRequest(byte []oid, int off, int len,
				    String operation, int requestId)
    throws IOException;

  public void writeRequestServiceControlList()
    throws IOException
  {
    write_long(1);
    writeCodeSetService();
  }

  public void writeCodeSetService()
    throws IOException
  {
    write_long(IiopReader.SERVICE_CODE_SET);
    write_long(12); // length
    write_long(0);
    write_long(0x10001); // iso-8859-1
    write_long(0x10100); // utf-16
  }
  
  /**
   * Writes the header for a request
   */
  abstract public void startReplyOk(int requestId)
    throws IOException;
  
  /**
   * Writes the header for a system reply exception
   */
  abstract public void startReplySystemException(int requestId,
						 String exceptionId,
						 int minorStatus,
						 int completionStatus)
    throws IOException;

  /**
   * Writes the header for a user exception
   */
  public void startReplyUserException(int requestId,
				      String exceptionId)
    throws IOException
  {
    startReplyUserException(requestId);

    writeString(exceptionId);
  }
  
  /**
   * Writes the header for a user exception with no exception id
   */
  abstract public void startReplyUserException(int requestId)
    throws IOException;

  /**
   * Writes an IOR to the output.
   */
  public void writeIOR(IOR ior)
  {
    byte []bytes = ior.getByteArray();

    _out.align(4);
    _out.write(bytes, 0, bytes.length);
  }

  /**
   * Writes a null IOR to the packet.
   */
  public void writeNullIOR()
  {
    write_long(1);
    write_long(0);
    write_long(0);
  }

  /**
   * Writes a null to the packet.
   */
  public void writeNull()
  {
    write_long(0);
  }

  /* CORBA */
  public org.omg.CORBA.ORB orb()
  {
    //
    ORB orb = ORB.init();

    return orb;
  }
  
  public org.omg.CORBA.portable.InputStream create_input_stream()
  {
    throw new UnsupportedOperationException("no input stream");
  }

  /**
   * Writes a sequence of booleans to the output stream.
   */
  public void write_boolean_array(boolean []value, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      _out.write(value[i + offset] ? 1 : 0);
  }

  /**
   * Writes a sequence of 8-bit characters to the output stream.
   */
  public void write_char_array(char []value, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      _out.write((int) value[i + offset]);
  }

  /**
   * Writes a sequence of 16-bit characters to the output stream.
   */
  public void write_wchar_array(char []value, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      write_wchar(value[i + offset]);
  }

  /**
   * Writes a sequence of bytes to the output stream.
   */
  public void write_octet_array(byte []value, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      _out.write((int) value[i + offset]);
  }

  /**
   * Writes a sequence of shorts to the output stream.
   */
  public void write_short_array(short []value, int offset, int length)
  {
    for (int i = 0; i < length; i++)
      _out.writeShort((int) value[i + offset]);
  }

  /**
   * Writes a sequence of unsigned shorts to the output stream.
   */
  public void write_ushort_array(short []value, int offset, int length)
  {
    _out.align(2);
    for (int i = 0; i < length; i++)
      _out.writeShort((int) value[i + offset]);
  }

  /**
   * Writes a sequence of CORBA long (Java int) to the output stream.
   */
  public void write_long_array(int []value, int offset, int length)
  {
    _out.align(4);
    for (int i = 0; i < length; i++)
      _out.writeInt(value[i + offset]);
  }

  /**
   * Writes a sequence of CORBA unsigned long (Java int) to the output stream.
   */
  public void write_ulong_array(int []value, int offset, int length)
  {
    _out.align(4);
    for (int i = 0; i < length; i++)
      _out.writeInt(value[i + offset]);
  }

  /**
   * Writes a sequence of CORBA long long (Java long) to the output stream.
   */
  public void write_longlong_array(long []value, int offset, int length)
  {
    _out.align(8);
    for (int i = 0; i < length; i++)
      _out.writeLong(value[i + offset]);
  }

  /**
   * Writes a sequence of CORBA long long (Java long) to the output stream.
   */
  public void write_ulonglong_array(long []value, int offset, int length)
  {
    _out.align(8);
    for (int i = 0; i < length; i++)
      _out.writeLong(value[i + offset]);
  }

  /**
   * Writes a sequence of floats to the output stream.
   */
  public void write_float_array(float []value, int offset, int length)
  {
    _out.align(4);
    for (int i = 0; i < length; i++) {
      float v = value[i + offset];
      int bits = Float.floatToIntBits(v);
      
      _out.writeInt(bits);
    }
  }

  /**
   * Writes a sequence of doubles to the output stream.
   */
  public void write_double_array(double []value, int offset, int length)
  {
    _out.align(8);
    for (int i = 0; i < length; i++) {
      double v = value[i + offset];
      long bits = Double.doubleToLongBits(v);
      
      _out.writeLong(bits);
    }
  }

  /**
   * Writes a sequence of 8-bit characters to the output stream.
   */
  public void write_string(String a)
  {
    if (a == null) {
      write_long(0);
      return;
    }
    
    int length = a.length();
    write_long(length + 1);
    for (int i = 0; i < length; i++)
      _out.write((int) a.charAt(i));
    _out.write(0);
  }

  /**
   * Writes a sequence of 8-bit characters to the output stream.
   */
  public void write_wstring(String a)
  {
    if (a == null) {
      write_long(0);
      return;
    }
    
    int length = a.length();
    write_long(length + 1);
    for (int i = 0; i < length; i++)
      _out.writeShort((int) a.charAt(i));
    _out.writeShort(0);
  }

  /**
   * Writes a CORBA object to the output stream.
   */
  public void write_Object(org.omg.CORBA.Object obj)
  {
    if (obj == null) {
      write_long(1);
      write(0);
      write_long(0);
    }
    else {
      writeIOR(((DummyObjectImpl) obj).getIOR());
    }
  }

  /**
   * Writes a CORBA typecode to the output stream.
   */
  public void write_TypeCode(TypeCode tc)
  {
    //System.out.println("WRITE: " + tc);
    if (tc == null) {
      write_long(TCKind._tk_null);
      return;
    }
      
    try {
      switch (tc.kind().value()) {
      case TCKind._tk_null:
      case TCKind._tk_void:
      case TCKind._tk_short:
      case TCKind._tk_ushort:
      case TCKind._tk_long:
      case TCKind._tk_ulong:
      case TCKind._tk_longlong:
      case TCKind._tk_ulonglong:
      case TCKind._tk_float:
      case TCKind._tk_double:
      case TCKind._tk_longdouble:
      case TCKind._tk_boolean:
      case TCKind._tk_char:
      case TCKind._tk_wchar:
      case TCKind._tk_octet:
      case TCKind._tk_any:
      case TCKind._tk_TypeCode:
	write_long(tc.kind().value());
	break;
	  
      case TCKind._tk_string:
	write_long(tc.kind().value());
	write_long(tc.length());
	break;
	  
      case TCKind._tk_sequence:
	write_long(tc.kind().value());
	write_TypeCode(tc.content_type());
	write_long(tc.length());
	break;
	  
      case TCKind._tk_value:
	// XXX: need to mark for recursive
	write_long(tc.kind().value());
	write_string(tc.id());
	write_string(tc.name());
	write_short(tc.type_modifier());
	write_TypeCode(tc.concrete_base_type());
	write_ulong(tc.member_count());
	for (int i = 0; i < tc.member_count(); i++) {
	  write_string(tc.member_name(i));
	  write_TypeCode(tc.member_type(i));
	  write_short(tc.member_visibility(i));
	}
	break;
	  
      case TCKind._tk_value_box:
	write_long(tc.kind().value());
	write_string(tc.id());
	write_string(tc.name());
	write_TypeCode(tc.content_type());
	break;
	  
      case TCKind._tk_abstract_interface:
	// XXX: need to mark for recursive
	write_long(tc.kind().value());
	write_string(tc.id());
	write_string(tc.name());
	break;
      
      default:
	System.out.println("UNKNOWN TC: " + tc + " " + tc.kind() + " " + tc.kind().value());
	throw new UnsupportedOperationException(String.valueOf(tc));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes a CORBA abstract interface to the output stream.
   */
  public void write_abstract_interface(java.lang.Object obj)
  {
    // XXX: check for remote object
    write_boolean(false);

    write_value((Serializable) obj);
  }
  
  public void write_any(Any any)
  {
    write_TypeCode(any.type());
    any.write_value(this);
  }
  
  public void write_Principal(org.omg.CORBA.Principal principal)
  {
    throw new UnsupportedOperationException();
  }

  public void write_value(Serializable obj, Class javaType)
  {
    write_value(obj);
  }

  public void write_value(Serializable obj)
  {
    if (obj == null) {
      write_long(0);
      return;
    }
    else if (obj instanceof String) {
      write_long(VALUE_TAG | VALUE_ONE_REP_ID);
      write_string("IDL:omg.org/CORBA/WStringValue:1.0");
      write_wstring((String) obj);
    }
    else if (obj instanceof Class) {
      write_long(VALUE_TAG | VALUE_ONE_REP_ID);
      write_string(valueHandler.getRMIRepositoryID(ClassDesc.class));
      write_value(valueHandler.getRMIRepositoryID((Class) obj));
      write_value(valueHandler.getRMIRepositoryID((Class) obj));
    }
    else {
      int oldValue = _refMap.get(obj);

      if (oldValue == _refMap.NULL) {
	_out.align(4);
	
	_refMap.put(obj, _out.getOffset());

	write_long(VALUE_TAG | VALUE_ONE_REP_ID);
	write_string(valueHandler.getRMIRepositoryID(obj.getClass()));
	valueHandler.writeValue(this, obj);
      }
      else {
	_out.align(4);

	write_long(0xffffffff);
	int delta = oldValue - _out.getOffset();
	
	write_long(delta);
      }
    }
  }

  /**
   * Writes a 32-bit integer.
   */
  public void write(int b)
  {
    write_long(b);
  }

  /**
   * Writes a boolean
   */
  public void write_boolean(boolean v)
  {
    _out.write(v ? 1 : 0);
  }

  /**
   * Writes a 8-bit byte.
   */
  public void write_octet(byte v)
  {
    _out.write(v);
  }

  /**
   * Writes a 16-bit short.
   */
  public void write_short(short v)
  {
    _out.align(2);
    _out.writeShort(v);
  }

  /**
   * Writes a 16-bit short.
   */
  public void write_ushort(short v)
  {
    _out.align(2);
    _out.writeShort(v);
  }

  /**
   * Writes a 8-bit char.
   */
  public void write_char(char v)
  {
    _out.write(v);
  }

  /**
   * Writes a 16-bit char.
   */
  public void write_wchar(char v)
  {
    _out.align(2);
    _out.writeShort(v);
  }

  /**
   * Writes a 32-bit int
   */
  public void write_long(int v)
  {
    _out.align(4);
    _out.writeInt(v);
  }

  /**
   * Writes a 32-bit int
   */
  public void write_ulong(int v)
  {
    _out.align(4);
    _out.writeInt(v);
  }

  /**
   * Writes a 64-bit int
   */
  public void write_longlong(long v)
  {
    _out.align(8);
    _out.writeLong(v);
  }

  /**
   * Writes a 64-bit long
   */
  public void write_ulonglong(long v)
  {
    _out.align(8);
    _out.writeLong(v);
  }

  /**
   * Writes a 32-bit float.
   */
  public void write_float(float v)
  {
    int bits = Float.floatToIntBits(v);

    _out.align(4);
    _out.writeInt(bits);
  }

  /**
   * Writes a 64-bit double.
   */
  public void write_double(double v)
  {
    long bits = Double.doubleToLongBits(v);

    _out.align(8);
    _out.writeLong(bits);
  }

  /**
   * Writes a string to the packet.
   *
   * @param v string value
   */
  public void writeString(String v)
  {
    if (v == null) {
      write_long(0);
      return;
    }

    int len = v.length();
    write_long(len + 1);
    for (int i = 0; i < len; i++)
      _out.write(v.charAt(i));
    _out.write(0);
  }

  /**
   * Writes a byte array
   *
   * @param b byte buffer
   */
  public void writeBytes(byte []b, int off, int len)
  {
    write_long(len);
    _out.write(b, off, len);
  }

  /**
   * Writes a byte array
   *
   * @param b byte buffer
   */
  public void write(byte []b, int off, int len)
  {
    _out.write(b, off, len);
  }

  public IiopReader _call()
    throws IOException
  {
    _out.close();

    _reader.readRequest();

    return _reader;
  }
}

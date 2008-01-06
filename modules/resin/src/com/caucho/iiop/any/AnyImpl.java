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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop.any;

import org.omg.CORBA.Any;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.Principal;

import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

import com.caucho.iiop.IiopReader;
import com.caucho.iiop.IiopWriter;

/**
 * Implementation of the IIOP Any
 */
public class AnyImpl extends Any
{
  private TypeCodeFactory _factory;
  
  private TypeCode _typeCode;
  private Object _value;

  public AnyImpl()
  {
  }

  public AnyImpl(TypeCodeFactory factory)
  {
    _factory = factory;
  }

  public InputStream create_input_stream()
  {
    throw new UnsupportedOperationException();
  }

  public OutputStream create_output_stream()
  {
    throw new UnsupportedOperationException();
  }

  public boolean equal(Any any)
  {
    return this == any;
  }

  public Any extract_any()
  {
    return (Any) _value;
  }

  public boolean extract_boolean()
  {
    return (Boolean) _value;
  }

  public char extract_char()
  {
    return (Character) _value;
  }

  public double extract_double()
  {
    return (Double) _value;
  }

  public java.math.BigDecimal extract_fixed()
  {
    return (java.math.BigDecimal) _value;
  }

  public float extract_float()
  {
    return (Float) _value;
  }

  public int extract_long()
  {
    return (Integer) _value;
  }

  public long extract_longlong()
  {
    return (Long) _value;
  }

  public org.omg.CORBA.Object extract_Object()
  {
    return (org.omg.CORBA.Object) _value;
  }

  public byte extract_octet()
  {
    return (Byte) _value;
  }

  public Principal extract_Principal()
  {
    throw new UnsupportedOperationException();
  }

  public short extract_short()
  {
    return (Short) _value;
  }

  public Streamable extract_Streamable()
  {
    return (Streamable) _value;
  }

  public String extract_string()
  {
    return (String) _value;
  }

  public TypeCode extract_TypeCode()
  {
    return (TypeCode) _value;
  }

  public int extract_ulong()
  {
    return (Integer) _value;
  }

  public long extract_ulonglong()
  {
    return (Long) _value;
  }

  public short extract_ushort()
  {
    return (Short) _value;
  }

  public java.io.Serializable extract_Value()
  {
    return (java.io.Serializable) _value;
  }

  public char extract_wchar()
  {
    return (Character) _value;
  }

  public String extract_wstring()
  {
    return (String) _value;
  }

  public void insert_any(Any v)
  {
    _value = v;
  }

  public void insert_boolean(boolean v)
  {
    _typeCode = BooleanTypeCode.TYPE_CODE;
    _value = v;
  }

  public void insert_char(char v)
  {
    _value = v;
  }

  public void insert_double(double v)
  {
    _value = v;
  }

  public void insert_fixed(java.math.BigDecimal v)
  {
    _value = v;
  }

  public void insert_fixed(java.math.BigDecimal v, TypeCode type)
  {
    throw new UnsupportedOperationException();
  }

  public void insert_float(float v)
  {
    _value = v;
  }

  public void insert_long(int v)
  {
    _value = v;
  }

  public void insert_longlong(long v)
  {
    _value = v;
  }

  public void insert_Object(org.omg.CORBA.Object v)
  {
    _value = v;
  }

  public void insert_Object(org.omg.CORBA.Object v, TypeCode t)
  {
    _value = v;
  }

  public void insert_octet(byte v)
  {
    _value = v;
  }

  public void insert_Principal(Principal v)
  {
    _value = v;
  }

  public void insert_short(short v)
  {
    _value = v;
  }

  public void insert_Streamable(Streamable v)
  {
    _value = v;
  }

  public void insert_string(String v)
  {
    _value = v;
  }

  public void insert_TypeCode(TypeCode v)
  {
    _value = v;
  }

  public void insert_ulong(int v)
  {
    _value = v;
  }

  public void insert_ulonglong(long v)
  {
    _value = v;
  }

  public void insert_ushort(short v)
  {
    _value = v;
  }

  public void insert_Value(java.io.Serializable v)
  {
    System.out.println("VALUE: " + v);
    _typeCode = _factory.createTypeCode(v.getClass());
    _value = v;
  }

  public void insert_Value(java.io.Serializable v, TypeCode t)
  {
    System.out.println("VALUE1: " + v + " " + t);
    _typeCode = t;
    _value = v;
  }

  public void insert_wchar(char v)
  {
    _value = v;
  }

  public void insert_wstring(String v)
  {
    _value = v;
  }

  public void read_value(InputStream is, TypeCode typeCode)
  {
    _typeCode = typeCode;
      
    _value = ((AbstractTypeCode) typeCode).readValue((IiopReader) is);
  }

  public TypeCode type()
  {
    return _typeCode;
  }

  public void type(TypeCode t)
  {
    _typeCode = t;
    _value = null;
  }

  public void write_value(OutputStream os)
  {
    System.out.println("WRITE: " + _typeCode + " " + _value);
    ((AbstractTypeCode) _typeCode).writeValue((IiopWriter) os, _value);
  }
}

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

package com.caucho.iiop;

import java.util.logging.Logger;

import java.io.Serializable;
import java.io.IOException;

import java.math.BigDecimal;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.Any;
import org.omg.CORBA.Principal;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_INV_ORDER;

import org.omg.CORBA.portable.Streamable;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import com.caucho.util.L10N;

import com.caucho.log.Log;

public class AnyImpl extends Any {
  protected static final L10N L = new L10N(AnyImpl.class);
  protected static final Logger log = Log.open(AnyImpl.class);

  private TypeCode _type;

  private Object _objValue;
  
  /**
   * Tests for equality.
   */
  public boolean equal(Any v)
  {
    return this == v;
  }
  
  /**
   * Return the type.
   */
  public TypeCode type()
  {
    return _type;
  }
  
  /**
   * Sets the type.
   */
  public void type(TypeCode t)
  {
    _type = t;
  }
  
  /**
   * Reads the value.
   */
  public void read_value(InputStream is, TypeCode typeCode)
    throws MARSHAL
  {
    try {
      _type = typeCode;
      
      Object value = ((TypeCodeImpl) typeCode).readValue((IiopReader) is);
    
      log.info("VALUE: " + value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Writes the value.
   */
  public void write_value(OutputStream os)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates an output stream.
   */
  public OutputStream create_output_stream()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates an input stream.
   */
  public InputStream create_input_stream()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a short value.
   */
  public short extract_short()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a short value.
   */
  public void insert_short(short v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an unsigned short value.
   */
  public short extract_ushort()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets an unsigned short value.
   */
  public void insert_ushort(short v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a long value.
   */
  public int extract_long()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a long value.
   */
  public void insert_long(int v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an unsigned long value.
   */
  public int extract_ulong()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets an unsigned long value.
   */
  public void insert_ulong(int v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a longlong value.
   */
  public long extract_longlong()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a longlong value.
   */
  public void insert_longlong(long v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an unsigned longlong value.
   */
  public long extract_ulonglong()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a longlong value.
   */
  public void insert_ulonglong(long v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a float value.
   */
  public float extract_float()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a float value.
   */
  public void insert_float(float v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a double value.
   */
  public double extract_double()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a double value.
   */
  public void insert_double(double v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a boolean value.
   */
  public boolean extract_boolean()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a boolean value.
   */
  public void insert_boolean(boolean v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a char value.
   */
  public char extract_char()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a char value.
   */
  public void insert_char(char v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a char value.
   */
  public char extract_wchar()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a char value.
   */
  public void insert_wchar(char v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a byte value.
   */
  public byte extract_octet()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a byte value.
   */
  public void insert_octet(byte v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a any value.
   */
  public Any extract_any()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a any value.
   */
  public void insert_any(Any v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an object value.
   */
  public org.omg.CORBA.Object extract_Object()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets an object value.
   */
  public void insert_Object(org.omg.CORBA.Object v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets an object value.
   */
  public void insert_Object(org.omg.CORBA.Object v, TypeCode t)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a value.
   */
  public Serializable extract_Value()
    throws BAD_OPERATION
  {
    return (Serializable) _objValue;
  }
  
  /**
   * Sets a value.
   */
  public void insert_Value(Serializable v)
  {
    _objValue = v;
  }
  
  /**
   * Sets a value.
   */
  public void insert_Value(Serializable v, TypeCode t)
    throws MARSHAL
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a string value.
   */
  public String extract_string()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a string value.
   */
  public void insert_string(String v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a string value.
   */
  public String extract_wstring()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a string value.
   */
  public void insert_wstring(String v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a TypeCode value.
   */
  public TypeCode extract_TypeCode()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a TypeCode value.
   */
  public void insert_TypeCode(TypeCode v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a Principal value.
   */
  public Principal extract_Principal()
    throws BAD_OPERATION
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a Principal value.
   */
  public void insert_Principal(Principal v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a Streamable value.
   */
  public Streamable extract_Streamable()
    throws BAD_INV_ORDER
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a Streamable value.
   */
  public void insert_Streamable(Streamable v)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns a fixed value.
   */
  public BigDecimal extract_fixed()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a fixed value.
   */
  public void insert_fixed(BigDecimal v)
  {
    throw new UnsupportedOperationException();
  }
}

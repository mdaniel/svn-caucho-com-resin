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

import java.io.IOException;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.Any;

import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import com.caucho.util.L10N;

import com.caucho.log.Log;

public class TypeCodeImpl extends TypeCode {
  protected static final L10N L = new L10N(TypeCodeImpl.class);
  protected static final Logger log = Log.open(TypeCodeImpl.class);

  public static final int TK_NULL = 0;
  public static final int TK_VOID = 1;
  public static final int TK_SHORT = 2;
  public static final int TK_LONG = 3;
  public static final int TK_USHORT = 4;
  public static final int TK_ULONG = 5;
  public static final int TK_FLOAT = 6;
  public static final int TK_DOUBLE = 7;
  public static final int TK_BOOLEAN = 8;
  public static final int TK_CHAR = 9;
  public static final int TK_OCTET = 10;
  public static final int TK_ANY = 11;
  public static final int TK_TYPE_CODE = 12;
  public static final int TK_PRINCIPAL = 13;
  public static final int TK_OBJREF = 14;
  public static final int TK_UNION = 16;
  public static final int TK_ENUM = 17;
  public static final int TK_STRING = 18;
  public static final int TK_SEQUENCE = 19;
  public static final int TK_ARRAY = 20;
  public static final int TK_ALIAS = 21;
  public static final int TK_EXCEPT = 22;
  public static final int TK_LONGLONG = 23;
  public static final int TK_ULONGLONG = 24;
  public static final int TK_LONGDOUBLE = 25;
  public static final int TK_WCHAR = 26;
  public static final int TK_WSTRING = 27;
  public static final int TK_FIXED = 28;
  public static final int TK_VALUE = 29;
  public static final int TK_VALUE_BOX = 30;
  public static final int TK_NATIVE = 31;
  public static final int TK_ABSTRACT_INTERFACE = 32;

  private TCKind _kind;

  TypeCodeImpl(TCKind kind)
  {
    if (kind == null)
      throw new NullPointerException();
    
    _kind = kind;
  }

  /**
   * Returns true if the type codes are equal.
   */
  public boolean equal(TypeCode tc)
  {
    return tc == this;
  }

  /**
   * Returns true if the type codes are equivalent.
   */
  public boolean equivalent(TypeCode tc)
  {
    return tc == this;
  }

  /**
   * Returns the compact typecode
   */
  public TypeCode get_compact_typecode()
  {
    return this;
  }

  /**
   * Returns the kind of the type code.
   */
  public TCKind kind()
  {
    return _kind;
  }

  /**
   * Returns the rep-id of the typecode.
   */
  public String id()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the simple name withing the scope.
   */
  public String name()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the number of fields in the type code
   */
  public int member_count()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the field name for the given index.
   */
  public String member_name(int index)
    throws BadKind, Bounds
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the typecode for the member type.
   */
  public TypeCode member_type(int index)
    throws BadKind, Bounds
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the label for the given index.
   */
  public Any member_label(int index)
    throws BadKind, Bounds
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the discriminator type for the given index.
   */
  public TypeCode discriminator_type()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the default member.
   */
  public int default_index()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the number of elements.
   */
  public int length()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the typecode for the content type.
   */
  public TypeCode content_type()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the number of digits in the fixed type.
   */
  public short fixed_digits()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the scale of the fixed type.
   */
  public short fixed_scale()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the visibility status of the given member.
   */
  public short member_visibility(int index)
    throws BadKind, Bounds
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the type modifier.
   */
  public short type_modifier()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the type code of the concrete base
   */
  public TypeCode concrete_base_type()
    throws BadKind
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Reads the type code.
   */
  public static TypeCodeImpl read(IiopReader reader)
    throws IOException
  {
    int tcKind = reader.readInt();

    // System.out.println("KIND: " + TCKind.from_int(tcKind));
    switch (tcKind) {
    case TK_VALUE_BOX:
      {
	int len = reader.read_sequence_length();
	int endian = reader.read_octet();

	String repId = reader.readString();
	String name = reader.readString();
	TypeCode typeCode = reader.read_TypeCode();

	return new ValueBoxTypeCode(TCKind.from_int(tcKind), typeCode);
      }
      
    case TK_ABSTRACT_INTERFACE:
      {
	int len = reader.read_sequence_length();
	
	int offset = reader.getOffset();
	int tail = offset + len;

	int endian = reader.read_octet();

	String repId = reader.readString();
	String name = reader.readString();
	
	/*
	int direction = reader.readInt();
	while (reader.getOffset() < tail) {
	  TypeCode subCode = reader.read_TypeCode();
	}
	*/

	return new AbstractBaseTypeCode(TCKind.from_int(tcKind));
      }

    case TK_STRING:
      {
	int maxLen = reader.read_ulong();
	log.info("string len: " + maxLen);
	return new TypeCodeImpl(TCKind.from_int(tcKind));
      }
      
    case 0xffffffff:
      throw new UnsupportedOperationException("INDIRECTION");
      
    default:
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Reads the value from the .
   */
  public Object readValue(IiopReader reader)
    throws IOException
  {
    switch (_kind.value()) {
    case TK_STRING:
      return reader.read_string();
      
    case TK_WSTRING:
      return reader.read_wstring();
      
    case TK_ABSTRACT_INTERFACE:
      return reader.read_abstract_interface();
      
    default:
      throw new UnsupportedOperationException(_kind.toString());
    }
  }
}

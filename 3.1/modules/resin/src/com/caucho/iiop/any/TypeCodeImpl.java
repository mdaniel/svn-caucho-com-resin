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

package com.caucho.iiop.any;

import com.caucho.util.L10N;
import com.caucho.iiop.IiopReader;

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.CORBA.TypeCodePackage.Bounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class TypeCodeImpl extends AbstractTypeCode
{
  protected static final L10N L = new L10N(TypeCodeImpl.class);
  protected static final Logger log
    = Logger.getLogger(TypeCodeImpl.class.getName());

  public static final TypeCode TK_NULL = new TypeCodeImpl(TCKind.tk_null);
  
  public static final int TK_VOID = 1;
  public static final int TK_SHORT = 2;
  
  public static final TypeCode TK_LONG = new TypeCodeImpl(TCKind.tk_long);
  
  public static final int TK_USHORT = 4;
  public static final int TK_ULONG = 5;
  public static final int TK_FLOAT = 6;
  public static final int TK_DOUBLE = 7;
  public static final int TK_BOOLEAN = 8;
  public static final int TK_CHAR = 9;
  
  public static final TypeCode TK_OCTET = new TypeCodeImpl(TCKind.tk_octet);

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

  private String _id;
  
  private short _typeModifier;
  private TypeCode _concreteBaseType;
  private ArrayList<Member> _members = new ArrayList<Member>();

  private TypeCode _contentType;
  private int _length;

  public TypeCodeImpl(TCKind kind)
  {
    if (kind == null)
      throw new NullPointerException();
    
    _kind = kind;
  }

  private TypeCodeImpl(TCKind kind, String repId, short modifier)
  {
    _kind = kind;
    _id = repId;
    _typeModifier = modifier;
  }

  private TypeCodeImpl(TCKind kind, String repId)
  {
    _kind = kind;
    _id = repId;
  }

  public static TypeCodeImpl createValue(String repId, String name, short modifier)
  {
    return new TypeCodeImpl(TCKind.tk_value, repId, modifier);
  }

  public static TypeCodeImpl createAbstractInterface(String repId, String name)
  {
    return new TypeCodeImpl(TCKind.tk_abstract_interface, repId);
  }

  public static TypeCodeImpl createSequence()
  {
    return new TypeCodeImpl(TCKind.tk_sequence);
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
    return _id;
  }

  /**
   * Returns the simple name withing the scope.
   */
  public String name()
    throws BadKind
  {
    return "";
  }
  
  /**
   * Returns the type modifier.
   */
  public short type_modifier()
    throws BadKind
  {
    return _typeModifier;
  }
  
  /**
   * Set the type code of the concrete base
   */
  public void setConcreteBaseType(TypeCode baseType)
  {
    // XXX:
    _concreteBaseType = baseType;
  }
  
  /**
   * Returns the type code of the concrete base
   */
  public TypeCode concrete_base_type()
    throws BadKind
  {
    return _concreteBaseType;
  }
  
  /**
   * Returns the number of fields in the type code
   */
  public int member_count()
    throws BadKind
  {
    return _members.size();
  }

  public void addMember(String name, TypeCode type, short visibility)
  {
    // XXX:
    _members.add(new Member(name, type, visibility));
  }

  /**
   * Returns the field name for the given index.
   */
  public String member_name(int index)
    throws BadKind, Bounds
  {
    return _members.get(index).getName();
  }

  /**
   * Returns the typecode for the member type.
   */
  public TypeCode member_type(int index)
    throws BadKind, Bounds
  {
    return _members.get(index).getType();
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
   * Returns the label for the given index.
   */
  public Any member_label(int index)
    throws BadKind, Bounds
  {
    return null;
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

  public void setLength(int length)
  {
    // XXX:
    _length = length;
  }
  
  /**
   * Returns the number of elements.
   */
  public int length()
    throws BadKind
  {
    return _length;
  }

  /**
   * Returns the typecode for the content type.
   */
  public void setContentType(TypeCode type)
  {
    // XXX:
    _contentType = type;
  }

  /**
   * Returns the typecode for the content type.
   */
  public TypeCode content_type()
    throws BadKind
  {
    return _contentType;
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
   * Reads the type code.
   */
  public static AbstractTypeCode read(IiopReader reader)
    throws IOException
  {
    int tcKind = reader.readInt();

    // System.out.println("KIND: " + TCKind.from_int(tcKind));
    switch (tcKind) {
    case TK_VALUE_BOX:
      {
	return ValueBoxTypeCode.readTypeCode(reader);
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
  {
    log.fine("READ: " + this);
    
    switch (_kind.value()) {
    case TCKind._tk_null:
    case TCKind._tk_void:
      return null;
      
    case TCKind._tk_long:
      return new Integer(reader.read_long());
      
    case TCKind._tk_ulong:
      return new Integer(reader.read_ulong());
      
    case TCKind._tk_string:
      return reader.read_string();
      
    case TCKind._tk_wstring:
      return reader.read_wstring();
      
    case TK_ABSTRACT_INTERFACE:
      return reader.read_abstract_interface();

    case TCKind._tk_value:
      return reader.read_value();

    case TCKind._tk_value_box:
      return reader.read_value();
      
    default:
      throw new UnsupportedOperationException(String.valueOf(this));
    }
  }

  public String toString()
  {
    return "TypeCodeImpl[" + _kind.value() + "]";
  }
  
  static class Member {
    private String _name;
    private TypeCode _type;
    private short _visibility;

    Member(String name, TypeCode type, short visibility)
    {
      _name = name;
      _type = type;
      _visibility = visibility;
    }

    String getName()
    {
      return _name;
    }

    TypeCode getType()
    {
      return _type;
    }

    short getVisibility()
    {
      return _visibility;
    }
  }

}

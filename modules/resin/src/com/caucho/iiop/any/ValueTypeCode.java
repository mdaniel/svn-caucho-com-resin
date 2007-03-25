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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop.any;

import com.caucho.iiop.*;

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

import java.util.*;

public class ValueTypeCode extends AbstractTypeCode
{
  private String _id;
  private String _name;
  private short _modifier;

  private AbstractTypeCode _baseTypeCode;
  
  private ArrayList<Member> _members = new ArrayList<Member>();
  
  ValueTypeCode(String id, String name)
  {
    _id = id;
    _name = name;
  }
  
  ValueTypeCode(String id, String name, short modifier)
  {
    _id = id;
    _name = name;
    _modifier = modifier;
  }

  /**
   * Returns the kind of the type code.
   */
  public TCKind kind()
  {
    return TCKind.tk_value;
  }

  @Override
  public String id()
  {
    return _id;
  }

  @Override
  public String name()
  {
    return _name;
  }

  public void setBaseTypeCode(AbstractTypeCode base)
  {
    _baseTypeCode = base;
  }
  
  /**
   * Returns the type code of the concrete base
   */
  @Override
  public TypeCode concrete_base_type()
  {
    return _baseTypeCode;
  }
  
  /**
   * Returns the type modifier
   */
  @Override
  public short type_modifier()
  {
    return _modifier;
  }

  public void addMember(String name, TypeCode type, short visibility)
  {
    _members.add(new Member(name, type, visibility));
  }
  
  /**
   * Returns the number of fields in the type code
   */
  public int member_count()
  {
    return _members.size();
  }

  /**
   * Returns the field name for the given index.
   */
  public String member_name(int index)
  {
    return _members.get(index).getName();
  }

  /**
   * Returns the typecode for the member type.
   */
  public TypeCode member_type(int index)
  {
    return _members.get(index).getType();
  }

  /**
   * Returns the visibility status of the given member.
   */
  public short member_visibility(int index)
  {
    return _members.get(index).getVisibility();
  }

  public static ValueTypeCode readTypeCode(IiopReader is)
  {
    // XXX: recursive
    int len = is.read_sequence_length();
    int endian = is.read_octet();
	
    String id = is.read_string();
    String name = is.read_string();
    short modifier = is.read_short();
	
    ValueTypeCode typeCode = new ValueTypeCode(id, name, modifier);
    //System.out.println("V: " + id);
    // XXX: save
	
    TypeCode base = is.read_TypeCode();
    typeCode.setBaseTypeCode((AbstractTypeCode) base);
    int count = is.read_ulong();
    for (int i = 0; i < count; i++) {
      String fieldName = is.read_string();
      TypeCode fieldType = is.read_TypeCode();
      short visibility = is.read_short();
      
      typeCode.addMember(fieldName, fieldType, visibility);

      //System.out.println("V: " + fieldName + " " + fieldType);
    }

    return typeCode;
  }

  public static void writeTypeCode(IiopWriter os, TypeCode tc)
  {
    try {
      os.write_long(tc.kind().value());

      EncapsulationMessageWriter encap
	= new EncapsulationMessageWriter();
      Iiop12Writer subOut = new Iiop12Writer();
    
      subOut.init(encap);
      subOut.write(0);
      
      subOut.write_string(tc.id());
      subOut.write_string(tc.name());
      subOut.write_short(tc.type_modifier());
      subOut.write_TypeCode(tc.concrete_base_type());
      subOut.write_ulong(tc.member_count());
      for (int i = 0; i < tc.member_count(); i++) {
	subOut.write_string(tc.member_name(i));
	subOut.write_TypeCode(tc.member_type(i));
	subOut.write_short(tc.member_visibility(i));
      }
    
      encap.close();
      os.write_long(encap.getOffset());
      encap.writeToWriter(os);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Reads the value from the stream.
   */
  public Object readValue(IiopReader is)
  {
    return is.read_value();
  }

  /**
   * Writes the value to the stream.
   */
  public void writeValue(IiopWriter os, Object value)
  {
    os.write_value((java.io.Serializable) value);
  }

  public String toString()
  {
    return "ValueTypeCode[" + _id + "]";
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

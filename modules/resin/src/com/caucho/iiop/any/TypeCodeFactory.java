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

import java.lang.reflect.*;
import java.util.HashMap;
import javax.rmi.CORBA.*;

public class TypeCodeFactory
{
  private static final ValueHandler _valueHandler = Util.createValueHandler();

  public static final HashMap<Class,AbstractTypeCode> _staticTypeCodeMap
    = new HashMap<Class,AbstractTypeCode>();

  public final HashMap<Class,AbstractTypeCode> _typeCodeMap
    = new HashMap<Class,AbstractTypeCode>();

  public AbstractTypeCode createTypeCode(Class cl)
  {
    if (cl == null)
      return NullTypeCode.TYPE_CODE;
    
    AbstractTypeCode typeCode = _staticTypeCodeMap.get(cl);

    if (typeCode != null)
      return typeCode;

    typeCode = _typeCodeMap.get(cl);

    if (typeCode != null)
      return typeCode;

    if (cl.isArray()) {
      AbstractTypeCode contentType = createTypeCode(cl.getComponentType());
      
      typeCode = new ValueBoxTypeCode(_valueHandler.getRMIRepositoryID(cl),
				      cl.getName(),
				      new SequenceTypeCode(contentType));
    }
    else {
      typeCode = introspectClass(cl);
    }
    
    _typeCodeMap.put(cl, typeCode);

    return typeCode;
  }

  private AbstractTypeCode introspectClass(Class type)
  {
    ValueTypeCode valueTypeCode
      = new ValueTypeCode(_valueHandler.getRMIRepositoryID(type),
			  type.getName());

    _typeCodeMap.put(type, valueTypeCode);

    if (! Object.class.equals(type.getSuperclass()))
      valueTypeCode.setBaseTypeCode(createTypeCode(type.getSuperclass()));

    for (Field field : type.getDeclaredFields()) {
      if (Modifier.isTransient(field.getModifiers()))
	continue;
      
      if (Modifier.isStatic(field.getModifiers()))
	continue;

      short visibility = 0;
      valueTypeCode.addMember(field.getName(),
			      createTypeCode(field.getType()),
			      visibility);
    }

    return valueTypeCode;
  }

  private static AbstractTypeCode valueBox(Class type,
					   AbstractTypeCode contentType)
  {
    ValueHandler valueHandler = Util.createValueHandler();
    
    return new ValueBoxTypeCode(valueHandler.getRMIRepositoryID(type),
				type.getName(),
				contentType);
  }

  private static AbstractTypeCode array(Class type)
  {
    ValueHandler valueHandler = Util.createValueHandler();

    Class componentType = type.getComponentType();
    AbstractTypeCode contentType = _staticTypeCodeMap.get(componentType);
    
    return new ValueBoxTypeCode(valueHandler.getRMIRepositoryID(type),
				type.getName(),
				new SequenceTypeCode(contentType));
  }
  
  static {
    ValueHandler valueHandler = Util.createValueHandler();
    
    _staticTypeCodeMap.put(boolean.class, BooleanTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Boolean.class,
			   valueBox(Boolean.class, BooleanTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(char.class, WcharTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Character.class,
			   valueBox(Character.class, WcharTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(byte.class, OctetTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Byte.class,
			   valueBox(Byte.class, OctetTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(short.class, ShortTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Short.class,
			   valueBox(Short.class, ShortTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(int.class, LongTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Integer.class,
			   valueBox(Integer.class, LongTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(long.class, LongLongTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Long.class,
			   valueBox(Long.class, LongLongTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(float.class, FloatTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Float.class,
			   valueBox(Float.class, FloatTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(double.class, DoubleTypeCode.TYPE_CODE);
    _staticTypeCodeMap.put(Double.class,
			   valueBox(Double.class, DoubleTypeCode.TYPE_CODE));
    
    _staticTypeCodeMap.put(String.class,
			   valueBox(String.class, StringTypeCode.TYPE_CODE));

    _staticTypeCodeMap.put(boolean[].class, array(boolean[].class));
    _staticTypeCodeMap.put(char[].class, array(char[].class));
    _staticTypeCodeMap.put(byte[].class, array(byte[].class));
    _staticTypeCodeMap.put(short[].class, array(short[].class));
    _staticTypeCodeMap.put(int[].class, array(int[].class));
    _staticTypeCodeMap.put(long[].class, array(long[].class));
    _staticTypeCodeMap.put(float[].class, array(float[].class));
    _staticTypeCodeMap.put(double[].class, array(double[].class));
  }
}

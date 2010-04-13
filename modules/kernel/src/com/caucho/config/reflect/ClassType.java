/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.reflect;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * class type matching
 */
@Module
public class ClassType extends BaseType
{
  public static final BaseType OBJECT_TYPE;
  
  private static final HashMap<Class<?>,ClassType> _classTypeMap
    = new HashMap<Class<?>,ClassType>();
  
  private static final HashSet<Class<?>> _classTypeIgnoreSet
    = new HashSet<Class<?>>();
    
  private Class<?> _type;

  protected ClassType(Class<?> type)
  {
    // ioc/0706
    /*
    Class<?> boxType = _boxTypeMap.get(type);

    if (boxType != null)
      type = boxType;
      */
    
    _type = type;
  }
  
  public static ClassType create(Class<?> type)
  {
    ClassType classType = _classTypeMap.get(type);
    
    if (classType != null)
      return classType;
    else
      return new ClassType(type);
  }
  
  @Override
  public Class<?> getRawClass()
  {
    return _type;
  }
  
  @Override
  public Type toType()
  {
    return _type;
  }
  
  @Override
  public boolean isMatch(Type type)
  {
    return _type.equals(type);
  }

  @Override
  public boolean isParamAssignableFrom(BaseType type)
  {
    if (_type.equals(type.getRawClass()))
      return true;
    else if (type.isWildcard())
      return true;
    else
      return false;
  }
    
  @Override
  public boolean isAssignableFrom(BaseType type)
  {
    if (! _type.isAssignableFrom(type.getRawClass()))
      return false;
    else if (type.getParameters().length > 0) {
      for (BaseType param : type.getParameters()) {
	if (! OBJECT_TYPE.isParamAssignableFrom(param))
	  return false;
      }

      return true;
    }
    else
      return true;
  }

  @Override
  public BaseType findClass(InjectManager manager, Class<?> cl)
  {
    if (_type.equals(cl))
      return this;

    for (Type type : _type.getGenericInterfaces()) {
      BaseType ifaceType = manager.createBaseType(type);

      BaseType baseType = ifaceType.findClass(manager, cl);

      if (baseType != null)
	return baseType;
    }

    Class<?> superclass = _type.getSuperclass();

    if (superclass == null)
      return null;

    BaseType superType = manager.createBaseType(superclass);

    return superType.findClass(manager, cl);
  }

  @Override
  public void fillTypeClosure(InjectManager manager, Set<Type> typeSet)
  {
    Type ownType = toType();
    
    if (_classTypeIgnoreSet.contains(ownType))
      return;
    
    typeSet.add(ownType);

    for (Type type : _type.getGenericInterfaces()) {
      BaseType ifaceType = manager.createBaseType(type);
      
      ifaceType.fillTypeClosure(manager, typeSet);
    }

    Class<?> superclass = _type.getSuperclass();

    if (superclass != null) {
      BaseType superType = manager.createBaseType(superclass);
      
      superType.fillTypeClosure(manager, typeSet);
    }
    else if (_type.isInterface()) {
      typeSet.add(Object.class);
    }
  }

  @Override
  public int hashCode()
  {
    return _type.hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (! (o instanceof ClassType))
      return false;

    ClassType type = (ClassType) o;

    return _type.equals(type._type);
  }

  @Override
  public String toString()
  {
    return getRawClass().toString();
  }

  static {
    _classTypeMap.put(boolean.class, new BoxType(boolean.class, Boolean.class));
    _classTypeMap.put(Boolean.class, new ClassType(Boolean.class));
    
    _classTypeMap.put(char.class, new BoxType(char.class, Character.class));
    _classTypeMap.put(Character.class, new ClassType(Character.class));
    
    _classTypeMap.put(byte.class, new BoxType(byte.class, Byte.class));
    _classTypeMap.put(Byte.class, new ClassType(Byte.class));
    
    _classTypeMap.put(short.class, new BoxType(short.class, Short.class));
    _classTypeMap.put(Short.class, new ClassType(Short.class));
    
    _classTypeMap.put(int.class, new BoxType(int.class, Integer.class));
    _classTypeMap.put(Integer.class, new ClassType(Integer.class));
    
    _classTypeMap.put(long.class, new BoxType(long.class, Long.class));
    _classTypeMap.put(Long.class, new ClassType(Long.class));
    
    _classTypeMap.put(float.class, new BoxType(float.class, Float.class));
    _classTypeMap.put(Float.class, new ClassType(Float.class));
    
    _classTypeMap.put(double.class, new BoxType(double.class, Double.class));
    _classTypeMap.put(Double.class, new ClassType(Double.class));
    
    _classTypeMap.put(Object.class, new ClassType(Object.class));
    _classTypeMap.put(String.class, new ClassType(String.class));
    
    OBJECT_TYPE = BaseType.create(Object.class, 
                                  new HashMap<String,BaseType>());
    
    _classTypeIgnoreSet.add(Serializable.class);
    _classTypeIgnoreSet.add(Cloneable.class);
  }
}

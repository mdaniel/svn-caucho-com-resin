/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.websocket.common;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.L10N;

/**
 * websocket client container
 */

public class TypeUtil
{
  private static final L10N L = new L10N(TypeUtil.class);
  
  public static Class<?> getParameterType(Class<?> handlerClass, Class<?> api)
  {
    HashMap<String,Type> paramMap = new HashMap<>();
    
    Class<?> type = getParameterType(handlerClass, api, paramMap);

    if (type == null) {
      throw new ConfigException(L.l("{0} doesn't have a proper parameter",
                                    handlerClass.getName()));
    }
    
    return type;
  }
  
  private static Class<?> getParameterType(Type type,
                                           Class<?> api,
                                           HashMap<String,Type> paramMap)
  {
    if (type == null) {
      return null;
    }
    
    if (type instanceof Class<?>) {
      Class<?> cl = (Class<?>) type;
      
      if (cl.equals(api)) {
        TypeVariable<?> []vars = api.getTypeParameters();
        
        Type subtype = paramMap.get(vars[0].getName());
        
        if (subtype instanceof Class<?>) {
          return (Class<?>) subtype;
        }
        else if (subtype instanceof TypeVariable) {
          TypeVariable var = (TypeVariable) subtype;
          
          System.out.println("VAR: " + var + " " + paramMap);
        }
        
        return null;
      }

      for (Type iface : cl.getGenericInterfaces()) {
        Class<?> paramType = getParameterType(iface, api, paramMap);
        
        if (paramType != null) {
          return paramType;
        }
      }
      
      return getParameterType(cl.getGenericSuperclass(), api, paramMap);
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      
      HashMap<String,Type> newParamMap = new HashMap<>(paramMap);
      Class<?> rawType = (Class<?>) pType.getRawType();
      Type []actualTypes = pType.getActualTypeArguments();
      TypeVariable<?> []vars = rawType.getTypeParameters();
      
      for (int i = 0; i < vars.length; i++) {
        String var = vars[i].getName();
        
        Type actualType = actualTypes[i];
        
        if (actualType instanceof TypeVariable<?>) {
          TypeVariable<?> actualVar = (TypeVariable<?>) actualType;
          
          if (paramMap.get(actualVar.getName()) != null) {
            actualType = paramMap.get(actualVar.getName());
          }
        }
        
        newParamMap.put(var, actualType);
      }
      
      return getParameterType(rawType, api, newParamMap);
    }
    
    return null;
  }
}

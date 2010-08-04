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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.util.concurrent.ConcurrentHashMap;

import com.caucho.util.FreeList;

/**
 * JavaEE resource program
 */
public class ResourceProgramManager {
  private final ConcurrentHashMap<TargetKey,ResourceInjectionTargetProgram> _programMap
    = new ConcurrentHashMap<TargetKey,ResourceInjectionTargetProgram>();
  
  private final FreeList<TargetKey> _freeList = new FreeList<TargetKey>(16); 
  
  public void addResource(ResourceInjectionTargetProgram resource)
  {
    TargetKey key = new TargetKey(resource.getTargetClass(),
                                  resource.getTargetName());

    _programMap.put(key, resource);
  }
  
  public ResourceInjectionTargetProgram 
  findResource(Class<?> targetClass, String targetName)
  {
    TargetKey key = _freeList.allocate();
    
    if (key == null)
      key = new TargetKey();
    
    key.init(targetClass, targetName);
    
    ResourceInjectionTargetProgram program;
    
    program = _programMap.get(key);
    
    _freeList.free(key);
    
    return program;
  }
  
  static class TargetKey {
    private Class<?> _targetClass;
    private String _targetName;
    
    TargetKey()
    {
    }
    
    TargetKey(Class<?> targetClass, String targetName)
    {
      _targetClass = targetClass;
      _targetName = targetName;
    }
    
    public void init(Class<?> targetClass, String targetName)
    {
      _targetClass = targetClass;
      _targetName = targetName;
    }
    
    @Override
    public int hashCode()
    {
      return 65521 * _targetClass.hashCode() + _targetName.hashCode();
    }
    
    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof TargetKey)) {
        return false;
      }
      
      TargetKey key = (TargetKey) o;
      
      return (_targetClass.equals(key._targetClass)
              && _targetName.equals(key._targetName));
    }
  }
}

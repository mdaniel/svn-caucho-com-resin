/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.env.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.LocalRepositoryMXBean;

class LocalRepositoryAdmin
  extends AbstractManagedObject implements LocalRepositoryMXBean
{
  private LocalRepositoryService _service;
  
  LocalRepositoryAdmin(LocalRepositoryService service)
  {
    _service = service;
    
    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public String getRootHash()
  {
    return _service.getRepositorySpi().getRepositoryRootHash();
  }
  
  @Override
  public Map<String,Map<String,String>> getTagMap()
  {
    Map<String,RepositoryTagEntry> tagMap;
    
    tagMap = _service.getRepositorySpi().getTagMap();
    
    TreeMap<String,Map<String,String>> resultMap
      = new TreeMap<String,Map<String,String>>();

    ArrayList<String> tags = new ArrayList<String>(tagMap.keySet());
    Collections.sort(tags);
    
    for (String tag : tags) {
      RepositoryTagEntry tagEntry = tagMap.get(tag);
      
      TreeMap<String,String> entryMap = new TreeMap<String,String>();
      resultMap.put(tag, entryMap);
      
      entryMap.put("root", tagEntry.getRoot());
      
      ArrayList<String> names = new ArrayList<String>(tagEntry.getAttributeMap().keySet());
      Collections.sort(names);
      
      for (String name : names) {
        entryMap.put(name, tagEntry.getAttributeMap().get(name));
      }
    }
    
    return resultMap;
  }
}
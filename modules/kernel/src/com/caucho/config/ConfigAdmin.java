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


package com.caucho.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ConfigMXBean;
import com.caucho.util.*;
import com.caucho.vfs.*;

public class ConfigAdmin extends AbstractManagedObject implements ConfigMXBean
{
  private static final Logger log
    = Logger.getLogger(ConfigAdmin.class.getName());
  
  private static final L10N L = new L10N(ConfigAdmin.class);
  
  private List<Path> _paths = new ArrayList<Path>();
  
  public ConfigAdmin()
  {
    
  }

  public void addPath(Path path)
  {
    if (path.toString().toLowerCase().endsWith(".license"))
      return;
    
    if (! _paths.contains(path))
      _paths.add(path);
  }

  @Override
  public Path []getPaths()
  {
    Path []array = new Path[_paths.size()];
    _paths.toArray(array);
    return array;
  }
  
  @Override
  public String getConfig(String pathName)
  {
    Path path = Vfs.lookup(pathName);
    
    if (! _paths.contains(path)) {
      log.fine(L.l("Attempt to get resource denied: {0} is not a recognized config file.", 
                   path.getNativePath()));
      return null;
    }
    
    if (! path.exists()) {
      log.fine(L.l("Attempt to get resource failed: {0} does not exist.", 
                   path.getNativePath()));
      return null;
    }
    
    if (! path.canRead()) {
      log.fine(L.l("Attempt to get resource failed: {0} can not be read.", 
                   path.getNativePath()));
      return null;
    }
    
    CharBuffer buffer = new CharBuffer(1024);
    
    ReadStream reader = null;
    
    try {
      reader = path.openRead();
      reader.readAll(buffer, (int)path.getLength());
    } catch (Exception e) {
      log.log(Level.FINE, L.l("Attempt to get resource failed: {0} can not be read.", 
                              path.getNativePath()), e);
      return null;
    } finally {
      IoUtil.close(reader);
    }
    
    return buffer.toString();
  }

  @Override
  public String getName()
  {
    return null;
  }

  void register()
  {
    registerSelf();
  }

  void unregister()
  {
    unregisterSelf();
  }
}

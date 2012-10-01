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

package com.caucho.env.vfs;

import java.io.IOException;

import com.caucho.vfs.Path;
import com.caucho.vfs.SchemeMap;
import com.caucho.vfs.SchemeRoot;

/**
 * Virtual path based on an expansion repository
 */
public class RepositoryScheme extends SchemeRoot
{
  private final Path _root;
  
  private RepositoryScheme(Path root)
  {
    _root = root;
  }
  
  public static Path create(String scheme, 
                            String tagId,
                            Path physicalRoot)
    throws IOException
  {
    SchemeMap map = physicalRoot.getSchemeMap();
    
    physicalRoot = physicalRoot.createRoot();
    
    Path root = new RepositoryPath(tagId, physicalRoot);
    
    RepositoryScheme schemeRoot = new RepositoryScheme(root);
    map.put(scheme, schemeRoot);
    
    return schemeRoot.getRoot();
  }
  
  @Override
  public Path getRoot()
  {
    return _root;
  }
}
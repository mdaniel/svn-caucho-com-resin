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
import java.util.Map;

import com.caucho.env.repository.RepositorySpi;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;

/**
 * Virtual path based on an expansion repository
 */
public class RepositoryPath extends Path
{
  private static final L10N L = new L10N(RepositoryPath.class);
  
  private RepositoryRoot _repository;
  private RepositoryPath _root;
  private Path _physicalPath;
  
  public RepositoryPath(String tagId,
                        Path physicalRoot)
    throws IOException
  {
    super((Path) null);
    
    RepositorySystem repositorySystem = RepositorySystem.getCurrent();
    
    if (repositorySystem == null)
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          RepositorySystem.class.getName()));
    
    physicalRoot.mkdirs();
    
    RepositorySpi repositorySpi = repositorySystem.getRepositorySpi();
    
    _repository = new RepositoryRoot(tagId, repositorySpi, physicalRoot);
    _physicalPath = physicalRoot;
    _root = this;
  }
  
  protected RepositoryPath(RepositoryRoot repository,
                           RepositoryPath root, 
                           Path physicalPath)
  {
    super(root);
   
    _repository = repository;
    _root = root;
    _physicalPath = physicalPath;
  }
  
  @Override
  public String getPath()
  {
    return _physicalPath.getPath();
  }

  @Override
  public String getScheme()
  {
    return "cloud";
  }

  @Override
  public Path schemeWalk(String userPath, Map<String, Object> newAttributes,
                         String newPath, int offset)
  {
    Path physicalPath = _physicalPath.schemeWalk(userPath,
                                                 newAttributes, 
                                                 newPath, 
                                                 offset);

    return new RepositoryPath(_repository, _root, physicalPath);
  }
  
  @Override
  public boolean exists()
  {
    update();
    
    return _physicalPath.exists();
  }
  
  @Override
  public boolean isFile()
  {
    update();
    
    return _physicalPath.isFile();
  }
  
  @Override
  public boolean isDirectory()
  {
    update();
    
    return _physicalPath.isDirectory();
  }
  
  @Override
  public boolean canRead()
  {
    update();
    
    return _physicalPath.canRead();
  }
  
  @Override
  public boolean canWrite()
  {
    return false;
  }
 
  @Override
  public long getLength()
  {
    update();
    
    return _physicalPath.getLength();
  }
  
  @Override
  public long getLastModified()
  {
    update();
    
    return _physicalPath.getLastModified();
  }
  
  @Override
  public long getCreateTime()
  {
    update();
    
    return _physicalPath.getCreateTime();
  }
  
  @Override
  public long getLastAccessTime()
  {
    update();
    
    return _physicalPath.getLastAccessTime();
  }
  
  @Override
  public long getCrc64()
  {
    return _physicalPath.getCrc64();
  }
  
  //
  // directory operations
  //
  
  @Override
  public String []list()
    throws IOException
  {
    update();
    
    String []list = _physicalPath.list();
    
    _physicalPath.clearStatusCache();
    
    return list;
  }
  
  //
  // file opening
  //
  
  @Override
  public StreamImpl openReadImpl()
    throws IOException
  {
    update();
    
    return _physicalPath.openReadImpl();
  }
  
  @Override
  public Path unwrap()
  {
    return _physicalPath.unwrap();
  }
  
  private void update()
  {
    _repository.update();
  }
}
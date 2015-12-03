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


import com.caucho.env.git.GitSystem;
import com.caucho.env.service.*;
import com.caucho.util.L10N;

public class LocalRepositoryService extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = GitSystem.START_PRIORITY + 1;
  
  private static final L10N L = new L10N(LocalRepositoryService.class);

  private FileRepository _fileRepository;
  
  private LocalRepositoryAdmin _admin;
  
  private LocalRepositoryService()
  {
    GitSystem git = GitSystem.getCurrent();
    if (git == null)
      throw new IllegalStateException(L.l("{0} is required for {1}",
          GitSystem.class.getSimpleName(), getClass().getSimpleName()));

    _fileRepository = new FileRepository(git);
  }
  
  public static LocalRepositoryService createAndAddService()
  {
    ResinSystem system = preCreate(LocalRepositoryService.class);
    
    LocalRepositoryService service = new LocalRepositoryService();
    system.addService(LocalRepositoryService.class, service);
    
    return service;
  }

  public static LocalRepositoryService getCurrent()
  {
    return ResinSystem.getCurrentService(LocalRepositoryService.class);
  }
  
  public Repository getRepository()
  {
    return _fileRepository;
  }
  
  public RepositorySpi getRepositorySpi()
  {
    return _fileRepository;
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    _fileRepository.start();
    
    _admin = new LocalRepositoryAdmin(this);
  }
  
  @Override
  public void stop()
  {
    _fileRepository.stop();
  }
}
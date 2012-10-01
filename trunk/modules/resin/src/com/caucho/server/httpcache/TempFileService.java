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

package com.caucho.server.httpcache;

import com.caucho.env.service.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Represents an inode to a temporary file.
 */
public class TempFileService extends AbstractResinSubSystem
{
  private static final L10N L = new L10N(TempFileService.class);
  
  private final TempFileManager _manager;
  
  public TempFileService(TempFileManager manager)
  {
    _manager = manager;
  }

  public static TempFileService createAndAddService()
  {
    RootDirectorySystem rootService = RootDirectorySystem.getCurrent();
    if (rootService == null)
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          TempFileService.class.getSimpleName(),
                                          RootDirectorySystem.class.getSimpleName()));

    Path dataDirectory = rootService.getDataDirectory();
    TempFileManager manager = new TempFileManager(dataDirectory.lookup("tmp"));

    return createAndAddService(manager);
  }
  
  public static TempFileService createAndAddService(TempFileManager manager)
  {
    ResinSystem system = preCreate(TempFileService.class);

    TempFileService service = new TempFileService(manager);
    system.addService(TempFileService.class, service);

    return service;
  }
  
  public static TempFileService getCurrent()
  {
    return ResinSystem.getCurrentService(TempFileService.class);
  }
  
  public TempFileManager getManager()
  {
    return _manager;
  }
  
  @Override
  public void stop()
    throws Exception
  {
    super.stop();
    
    _manager.close();
    
  }
}

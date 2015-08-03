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

package com.caucho.env.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.java.WorkDir;
import com.caucho.util.L10N;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;

/**
 * Root service for the root and data directories.
 *
 */
public class RootDirectorySystem extends AbstractResinSubSystem 
{
  private static final Logger log
    = Logger.getLogger(RootDirectorySystem.class.getName());
  
  public static final int START_PRIORITY_ROOT_DIRECTORY = 20;

  private static final L10N L = new L10N(RootDirectorySystem.class);
  
  private final Path _rootDirectory;
  private final Path _dataDirectory;

  private FileOutputStream _foutLock;

  private FileLock _fileLock;

  public RootDirectorySystem(Path rootDirectory, Path dataDirectory) 
    throws IOException
  {
    if (rootDirectory == null) {
      throw new NullPointerException();
    }
    
    if (dataDirectory == null) {
      throw new NullPointerException();
    }
    
    if (dataDirectory instanceof MemoryPath) { // QA
      dataDirectory = 
        WorkDir.getTmpWorkDir().lookup("qa/" + dataDirectory.getFullPath());
    }
    
    _rootDirectory = rootDirectory;
    _dataDirectory = dataDirectory;
    
    rootDirectory.mkdirs();
    dataDirectory.mkdirs();
  }
  
  public static RootDirectorySystem createAndAddService(Path rootDirectory)
      throws IOException
  {
    return createAndAddService(rootDirectory, 
                               rootDirectory.lookup("resin-data"));
  }

  public static RootDirectorySystem createAndAddService(Path rootDirectory,
                                                         Path dataDirectory)
    throws IOException
  {
    ResinSystem system = preCreate(RootDirectorySystem.class);
    
    RootDirectorySystem service =
      new RootDirectorySystem(rootDirectory, dataDirectory);
    system.addService(RootDirectorySystem.class, service);
    
    return service;
  }

  public static RootDirectorySystem getCurrent()
  {
    return ResinSystem.getCurrentService(RootDirectorySystem.class);
  }

  /**
   * Returns the data directory for current active directory service.
   */
  public static Path getCurrentDataDirectory()
  {
    RootDirectorySystem rootService = getCurrent();
    
    if (rootService == null)
      throw new IllegalStateException(L.l("{0} must be active for getCurrentDataDirectory().",
                                          RootDirectorySystem.class.getSimpleName()));
    
    return rootService.getDataDirectory();
  }
  
  /**
   * Returns the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Returns the internal data directory.
   */
  public Path getDataDirectory()
  {
    return _dataDirectory;
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_ROOT_DIRECTORY;
  }
  
  @Override
  public void start() throws Exception
  {
    super.start();
    
    Path lockPath = getDataDirectory().lookup("resin.lock");
    
    try {
      File file = new File(lockPath.getNativePath());
      
      _foutLock = new FileOutputStream(file);
      try {
        _fileLock = _foutLock.getChannel().lock();
      } catch (IOException e) {
        throw new ConfigException(L.l("Can't obtain unique lock to {0}. Check for other active Resin processes.",
                                      lockPath.getNativePath()));
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  @Override
  public void stop() throws Exception
  {
    super.stop();
    
    try {
      if (_fileLock != null) {
        try {
          // Reflection is for JDK 6 compilation
          Method closeMethod = _fileLock.getClass().getMethod("close");
        
          if (closeMethod != null) {
            closeMethod.invoke(_fileLock);
          }
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
      
      if (_foutLock != null) {
        _foutLock.close();
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}

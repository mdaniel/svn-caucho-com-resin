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

package com.caucho.db.io;

import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;

/**
 * Filesystem access for a random-access store.
 * 
 * The store is designed around a single writer thread and multiple
 * reader threads. When possible, it uses mmap.
 */
public class StoreBuilder
{
  private Path _path;
  
  private boolean _isMmap = true;
  private boolean _isJniMmap = false;
  // private ServiceManagerAmp _rampManager;
  
  public StoreBuilder(Path path)
  {
    if (path == null) {
      throw new NullPointerException();
    }
    
    _path = path;
  }

  public Path getPath()
  {
    return _path;
  }
  
  public StoreBuilder mmap(boolean isMmap)
  {
    _isMmap = isMmap;
    
    return this;
  }
  
  public boolean isMmap()
  {
    return _isMmap;
  }
  
  public StoreBuilder jniMmap(boolean isJniMmap)
  {
    _isJniMmap = isJniMmap;
    
    return this;
  }
  
  public boolean isJniMmap()
  {
    return _isJniMmap;
  }
  
  /*
  public StoreBuilder rampManager(ServiceManagerAmp rampManager)
  {
    Objects.requireNonNull(rampManager);
    
    _rampManager = rampManager;
    
    return this;
  }
  
  public ServiceManagerAmp getRampManager()
  {
    return _rampManager;
  }
    */                              
  
  public StoreReadWrite build()
  {
    /*
    if (_rampManager == null) {
      _rampManager = Amp.newManager();
    }
    */
    
    StoreReadWrite store;
    
    if (isMmap() && CauchoSystem.isJdk7()) {
      store = new StoreReadWriteMmapNio(this);
    }
    else {
      store = new StoreReadWriteImpl(this);
    }

    return store;
  }
}

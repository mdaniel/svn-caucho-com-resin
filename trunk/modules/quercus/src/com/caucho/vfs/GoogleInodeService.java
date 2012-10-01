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
 * @author Nam Nguyen
 */

package com.caucho.vfs;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.util.IoUtil;
import com.caucho.vfs.GoogleInode.FileType;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleInodeService
{
  private static final Logger log = Logger.getLogger(GoogleInodeService.class.getName());

  private final MemcacheService _memcacheService;

  public GoogleInodeService(String namespace)
  {
    this(MemcacheServiceFactory.getMemcacheService(namespace));
  }

  public GoogleInodeService(MemcacheService memcacheService)
  {
    _memcacheService = memcacheService;
  }

  public GoogleInode getGoogleInode(GooglePath path)
  {
    HashMap<String,GoogleInode> dirMap = readDirMap(path.getParent());

    GoogleInode gsInode = null;
    String name = path.getTail();

    if (dirMap != null) {
      gsInode = dirMap.get(name);
    }

    if (gsInode == null) {
      gsInode = new GoogleInode(name, FileType.NONE, -1, -1);
    }

    return gsInode;
  }

  public HashMap<String,GoogleInode> readDirMap(GooglePath path)
  {
    String fullPath = path.getFullPath();

    HashMap<String,GoogleInode> dirMap
      = (HashMap<String,GoogleInode>) _memcacheService.get(fullPath);

    if (dirMap != null) {
      return dirMap;
    }

    ReadStream is = null;

    try {
      is = path.openRead();

      Hessian2Input hIn = new Hessian2Input(is);
      dirMap = (HashMap<String,GoogleInode>) hIn.readObject();
      hIn.close();

      boolean result = _memcacheService.put(fullPath, dirMap, null,
                                            MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);

      return dirMap;
    }
    catch (FileNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
    catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
    finally {
      IoUtil.close(is);
    }
  }

  public boolean writeDirMap(GooglePath path,
                             HashMap<String,GoogleInode> dirMap)
  {
    String fullPath = path.getFullPath();

    // XXX: distributed locking?

    WriteStream out = null;
    try {
      out = path.openWrite();

      Hessian2Output hOut = new Hessian2Output(out);
      hOut.writeObject(dirMap);
      hOut.close();

      _memcacheService.put(fullPath, dirMap);

      // don't silently close remote write streams except on error
      out.close();

      return true;
    }
    catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);

      IoUtil.close(out);

      return false;
    }
  }
}

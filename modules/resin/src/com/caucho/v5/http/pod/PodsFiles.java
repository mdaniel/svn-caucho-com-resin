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

package com.caucho.v5.http.pod;

import io.baratine.files.BfsFileSync;
import io.baratine.files.Status;

import java.util.Arrays;

import com.caucho.v5.bartender.pod.PodsManagerService;

/**
 * Manages the dependencies for the pod files.
 */
public class PodsFiles
{
  private PodsItem []_files;
  
  public PodsFiles(BfsFileSync dir, String []list)
  {
    Arrays.sort(list);
    
    _files = new PodsItem[list.length];
    
    for (int i = 0; i < list.length; i++) {
      String name = list[i];
      
      BfsFileSync file = dir.lookup(name);
      
      Status status = file.getStatus();
      
      _files[i] = new PodsItem(name, status.getVersion(), status.getChecksum());
    }
  }
  
  String []getFileNames()
  {
    String []fileNames = new String[_files.length];
    
    for (int i = 0; i < fileNames.length; i++) {
      fileNames[i] = _files[i].getName();
    }
    
    return fileNames;
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof PodsFiles)) {
      return false;
    }
    
    PodsFiles files = (PodsFiles) o;
    
    return equals(files, false);
  }

  public boolean equalsIgnoreAutopod(PodsFiles files)
  {
    return equals(files, true);
  }
  
  private boolean equals(PodsFiles files, boolean isExcludeAutoPod)
  {
    if (_files.length != files._files.length) {
      return false;
    }
    
    for (int i = 0; i < _files.length; i++) {
      PodsItem fileA = _files[i]; 
      PodsItem fileB  = files._files[i];
      
      if (! fileA.getName().equals(fileB.getName())) {
        return false;
      }
      else if (isExcludeAutoPod
               && fileA.getName().equals(PodsManagerService.PATH_AUTOPOD)) {
      }
      else if (fileA.getChecksum() != fileB.getChecksum()) {
        return false;
      }
    }
    
    return true;
  }
  
  private static class PodsItem {
    private String _name;
    private long _version;
    private long _checksum;
    
    PodsItem(String name,
             long version,
             long checksum)
    {
      _name = name;
      _version = version;
      _checksum = checksum;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public long getVersion()
    {
      return _version;
    }
    
    public long getChecksum()
    {
      return _checksum;
    }
  }
}

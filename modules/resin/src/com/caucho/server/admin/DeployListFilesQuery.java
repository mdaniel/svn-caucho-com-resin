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

package com.caucho.server.admin;

import java.util.*;

import com.caucho.vfs.StreamSource;

public class DeployListFilesQuery implements java.io.Serializable
{
  private String _tag;
  private String _fileName;
  private String []_fileList;

  public DeployListFilesQuery()
  {
  }

  public DeployListFilesQuery(String tag, String fileName)
  {
    _tag = tag;
    _fileName = fileName;
  }

  public DeployListFilesQuery(String tag, String fileName, String []fileList)
  {
    _tag = tag;
    _fileName = fileName;
    _fileList = fileList;
  }

  public String getTag()
  {
    return _tag;
  }
  
  public String getFileName()
  {
    return _fileName;
  }
  
  public String []getFileList()
  {
    return _fileList;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _tag + "," + _fileName + "]";
  }
}
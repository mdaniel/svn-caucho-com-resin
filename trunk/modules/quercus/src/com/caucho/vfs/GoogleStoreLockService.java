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

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.files.GSFileOptions.GSFileOptionsBuilder;

import java.io.IOException;

public class GoogleStoreLockService
{
  private FileService _fileService;
  private String _bucket;

  public GoogleStoreLockService(FileService fileService, String bucket)
  {
    _fileService = fileService;

    _bucket = bucket;
  }

  public GoogleLock lock(String key)
  {
    try {
      GSFileOptionsBuilder builder = new GSFileOptionsBuilder();
      builder.setMimeType("application/octet-stream");
      builder.setBucket(_bucket);

      key = key.substring(1);

      builder.setKey(key + ".LOCK");

      AppEngineFile file = _fileService.createNewGSFile(builder.build());

      boolean isLock = true;
      FileWriteChannel os = _fileService.openWriteChannel(file, isLock);

      return new GoogleLock(os);
    }
    catch (IOException e) {
      return null;
    }
  }
}

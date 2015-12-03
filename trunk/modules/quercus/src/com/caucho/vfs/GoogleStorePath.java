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
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.files.GSFileOptions.GSFileOptionsBuilder;

import java.io.IOException;

public class GoogleStorePath extends GooglePath
{
  private final String _bucket;

  private GoogleStorePath(FilesystemPath root, String userPath, String path,
                          FileService fileService,
                          GoogleInodeService inodeService)
  {
    super(root, userPath, path, fileService, inodeService);

    GoogleStorePath gsRoot = (GoogleStorePath) root;

    _bucket = gsRoot._bucket;
  }

  public GoogleStorePath()
  {
    this(FileServiceFactory.getFileService(),
         new GoogleInodeService("quercus"),
         null);
  }

  public GoogleStorePath(String bucket)
  {
    this(FileServiceFactory.getFileService(),
         new GoogleInodeService("quercus_" + bucket),
         bucket);
  }

  public GoogleStorePath(FileService fileService,
                         GoogleInodeService inodeService,
                         String bucket)
  {
    super(fileService, inodeService);

    try {
      if (bucket == null) {
        bucket = _fileService.getDefaultGsBucketName();
      }

      _bucket = bucket;

    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private GoogleStorePath(GoogleStorePath path)
  {
    super(path);

    _bucket = path._bucket;
  }

  public String getBucket()
  {
    return _bucket;
  }

  @Override
  public GooglePath createInstance(FilesystemPath root,
                                   String userPath,
                                   String path)
  {
    return new GoogleStorePath(root, userPath, path,
                               _fileService, _inodeService);
  }

  @Override
  protected boolean removeImpl()
  {
    return true;
  }

  @Override
  public AppEngineFile getAppEngineFile()
  {
    String path = getNativePath();

    AppEngineFile file = new AppEngineFile(path);

    return file;
  }

  @Override
  public String getNativePath()
  {
    String fullPath = getFullPath();

    if ("".equals(fullPath) || "/".equals(fullPath)) {
      fullPath = "/" + QUERCUS_ROOT_PATH;
    }

    return "/gs/" + _bucket + fullPath;
  }

  @Override
  public StreamImpl openWriteImpl() throws IOException
  {
    // System.out.println("XX-WRITE: " + getFullPath());
    if (! getParent().isDirectory() && ! getParent().mkdirs()) {
      //System.out.println("XX-WRITE-FAIL: " + getFullPath());
      throw new IOException(L.l("{0} must have a directory parent",
                                getParent()));
    }

    GSFileOptionsBuilder builder = new GSFileOptionsBuilder();
    builder.setMimeType("application/octet-stream");
    builder.setBucket(_bucket);

    String key = getFullPath();

    key = key.substring(1);

    if (key.equals("")) {
      key = QUERCUS_ROOT_PATH;
    }

    builder.setKey(key);

    AppEngineFile file = _fileService.createNewGSFile(builder.build());

    boolean isLock = true;
    FileWriteChannel os = _fileService.openWriteChannel(file, isLock);

    return new GoogleWriteStream(this, os, getGoogleInode());
  }

  public StreamImpl openAppendImpl() throws IOException
  {
    long longLength = getLength();

    if (longLength > Integer.MAX_VALUE) {
      return super.openAppendImpl();
    }

    int len = (int) longLength;
    if (len <= 0) {
      return openWriteImpl();
    }

    byte[] buffer = new byte[(int) len];

    ReadStream is = openRead();
    int totalRead = 0;

    try {
      int index = 0;
      int subLen;
      while ((subLen = is.read(buffer, index, len - totalRead)) >= 0) {
        index += subLen;
        totalRead += subLen;
      }
    }
    finally {
      is.close();
    }

    if (totalRead != len) {
      throw new IOException(L.l("expected {0} bytes but read only {0} bytes",
                                len, totalRead));
    }

    StreamImpl stream = openWriteImpl();
    GoogleWriteStream os = (GoogleWriteStream) stream;

    try {
      os.write(buffer, 0, totalRead, false);
    }
    catch (IOException e) {
      os.close();

      throw e;
    }

    return os;
  }

  @Override
  public Path copy()
  {
    return new GoogleStorePath(this);
  }
}

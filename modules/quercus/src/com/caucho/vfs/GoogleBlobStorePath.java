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

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreFailureException;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileWriteChannel;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// XXX: doesn't work, needs a datastore-backed metadata
//
public class GoogleBlobStorePath extends GooglePath
{
  private static Logger log = Logger.getLogger(GoogleBlobStorePath.class.getName());

  private GoogleBlobStorePath(FilesystemPath root,
                              String userPath,
                              String path,
                              FileService fileService,
                              GoogleInodeService inodeService)
  {
    super(root, userPath, path, fileService, inodeService);
  }

  public GoogleBlobStorePath(FileService fileService,
                             GoogleInodeService inodeService)
  {
    super(fileService, inodeService);

    //init();
  }

  @Override
  protected GooglePath createInstance(FilesystemPath root,
                                      String userPath,
                                      String path)
  {
    return new GoogleBlobStorePath(root, userPath, path,
                                   _fileService, _inodeService);
  }

  @Override
  protected boolean removeImpl()
  {
    BlobstoreService service = BlobstoreServiceFactory.getBlobstoreService();

    BlobKey key = new BlobKey(getNativePath());

    try {
      service.delete(key);
    }
    catch (BlobstoreFailureException e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }

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

    return "/blobstore" + fullPath;
  }

  @Override
  public StreamImpl openWriteImpl() throws IOException
  {
    if (! getParent().isDirectory() && ! getParent().mkdirs()) {
      throw new IOException(L.l("{0} must have a directory parent",
                                getParent()));
    }

    //if (exists()) {
    //  remove();
    //}

    AppEngineFile file = getAppEngineFile();

    boolean isLock = true;
    try {
      FileWriteChannel os = _fileService.openWriteChannel(file, isLock);

      return new GoogleWriteStream(this, os, getGoogleInode());
    }
    catch (IOException e) {
      throw e;
    }
  }
}

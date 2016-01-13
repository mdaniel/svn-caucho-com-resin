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
 * @author Nam Nguyen
 */

package com.caucho.v5.bartender.files;

import io.baratine.db.BlobReader;
import io.baratine.files.BfsFileSync;
import io.baratine.files.Status;
import io.baratine.files.Watch;
import io.baratine.files.WriteOption;
import io.baratine.service.Cancel;
import io.baratine.service.Result;
import io.baratine.service.ServiceManager;

import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.v5.baratine.ServiceServer;

class BfsClientImpl implements BfsClient
{
  private ServiceServer _server;
  private BfsFileSyncAmp _fileService;

  BfsClientImpl(ServiceServer server)
  {
    _server = server;

    ServiceManager manager = _server.client();

    _fileService = manager.service("bfs://cluster").as(BfsFileSyncAmp.class);
  }

  @Override
  public Status getStatus()
  {
    return _fileService.getStatus();
  }

  @Override
  public void getStatus(Result<Status> result)
  {
    _fileService.getStatus(result);
  }

  /*
  @Override
  public void getStatusSafe(Result<Status> result)
  {
    _fileService.getStatusSafe(result);
  }
  */

  @Override
  public BfsFileSync lookup(String relativePath)
  {
    return _fileService.lookup(relativePath);
  }

  @Override
  public String[] list()
  {
    return _fileService.list();
  }

  @Override
  public void list(Result<String[]> result)
  {
    _fileService.list(result);
  }

  @Override
  public InputStream openRead()
  {
    return _fileService.openRead();
  }

  @Override
  public void openRead(Result<InputStream> is)
  {
    _fileService.openRead(is);
  }

  @Override
  public BlobReader openReadBlob()
  {
    return _fileService.openReadBlob();
  }

  @Override
  public void openReadBlob(Result<BlobReader> result)
  {
    _fileService.openReadBlob(result);
  }

  @Override
  public OutputStream openWrite(WriteOption... options)
  {
    return _fileService.openWrite(options);
  }

  @Override
  public void openWrite(Result<OutputStream> result, WriteOption... options)
  {
    _fileService.openWrite(result, options);
  }

  @Override
  public void copyTo(String relPath,
                     Result<Boolean> result, 
                     WriteOption... options)
  {
    _fileService.copyTo(relPath, result, options);
  }

  @Override
  public boolean copyTo(String relPath,
                        WriteOption... options)
  {
    return _fileService.copyTo(relPath, options);
  }

  @Override
  public void renameTo(String relPath,
                       Result<Boolean> result, 
                       WriteOption... options)
  {
    _fileService.renameTo(relPath, result, options);
  }

  @Override
  public boolean renameTo(String relPath,
                          WriteOption... options)
  {
    return _fileService.renameTo(relPath, options);
  }

  @Override
  public void remove()
  {
    _fileService.remove();
  }

  @Override
  public void remove(Result<Boolean> result)
  {
    _fileService.remove(result);
  }

  @Override
  public void removeAll(Result<Boolean> result)
  {
    _fileService.removeAll(result);
  }

  @Override
  public void watch(Watch watch, Result<Cancel> result)
  {
    _fileService.watch(watch, result);
  }

  @Override
  public Cancel watch(Watch watch)
  {
    return _fileService.watch(watch);
  }

  /*
  @Override
  public void unregisterWatch(Watch watch)
  {
    _fileService.unregisterWatch(watch);
  }
  */

  @Override
  public void close() throws Exception
  {
    _server.close();
  }
}

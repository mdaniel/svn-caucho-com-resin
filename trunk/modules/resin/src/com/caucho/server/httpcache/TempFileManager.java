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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.db.Database;
import com.caucho.db.block.BlockStore;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStreamApi;
import com.caucho.vfs.Vfs;

/**
 * Represents an inode to a temporary file.
 */
public class TempFileManager
{
  private static final L10N L = new L10N(TempFileManager.class);
  private static final Logger log
    = Logger.getLogger(TempFileManager.class.getName());

  private final BlockStore _store;

  public TempFileManager(Path path)
  {
    try {
      path.getParent().mkdirs();

      String name = "temp_file";

      Path storePath = path.lookup(name);

      storePath.remove();

      if (storePath.exists()) {
        log.warning(L.l("Removal of old temp file '{0}' failed. Please check permissions.",
                        storePath.getNativePath()));
      }
      
      Database database = new Database();
      database.ensureMemoryCapacity(1024 * 1024);
      database.init();

      _store = new BlockStore(database, name, null, storePath);
      _store.setFlushDirtyBlocksOnCommit(false);
      _store.create();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  public void close()
  {
    _store.close();
  }

  public TempFileInode createInode()
  {
    return new TempFileInode(_store);
  }

  public TempStreamApi createTempStream()
  {
    TempFileInode inode = createInode();
    
    return new TempStreamImpl(inode);
  }
  
  class TempStreamImpl implements TempStreamApi {
    private TempFileInode _inode;
    private OutputStream _os;
    
    TempStreamImpl(TempFileInode inode)
    {
      _inode = inode;
      _os = inode.openOutputStream();
    }

    @Override
    public ReadStream openRead() throws IOException
    {
      OutputStream os = _os;
      
      if (os != null)
        os.close();
      
      return Vfs.openRead(_inode.openInputStream());
    }

    @Override
    public void write(byte[] buffer, int offset, int length, boolean isEnd)
        throws IOException
    {
      _os.write(buffer, offset, length);
    }
    
    @Override
    public void destroy()
    {
      TempFileInode inode = _inode;
      _inode = null;
      
      if (inode != null)
        inode.free();
    }
  }
}

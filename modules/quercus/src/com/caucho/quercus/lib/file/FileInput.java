/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.file;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;

import com.caucho.vfs.Path;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.ReadStream;

/**
 * Represents a Quercus file open for reading
 */
public class FileInput extends ReadStreamInput implements LockableStream {
  private static final Logger log
    = Logger.getLogger(FileInput.class.getName());

  private Path _path;
  private FileLock _fileLock;
  private FileChannel _fileChannel;

  public FileInput(Path path)
    throws IOException
  {
    _path = path;

    init(path.openRead());
  }

  /**
   * Returns the path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Opens a copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    return new FileInput(_path);
  }

  /**
   * Returns the number of bytes available to be read, 0 if no known.
   */
  public long getLength()
  {
    return getPath().getLength();
  }

  /**
   * Lock the shared advisory lock.
   */
  public boolean lock(boolean shared, boolean block)
  {
    if (! (getPath() instanceof FilePath))
      return false;

    try {
      File file = ((FilePath) getPath()).getFile();

      if (_fileChannel == null) {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

        _fileChannel = randomAccessFile.getChannel();
      }

      if (block)
        _fileLock = _fileChannel.lock(0, Long.MAX_VALUE, shared);
      else 
        _fileLock = _fileChannel.tryLock(0, Long.MAX_VALUE, shared);

      return _fileLock != null;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Unlock the advisory lock.
   */
  public boolean unlock()
  {
    try {
      if (_fileLock != null) {
        _fileLock.release();

        return true;
      }

      return false;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return "FileInput[" + getPath() + "]";
  }
}


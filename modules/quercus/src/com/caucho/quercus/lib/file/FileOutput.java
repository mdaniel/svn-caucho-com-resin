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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP open file
 */
public class FileOutput extends AbstractBinaryOutput implements LockableStream {
  private static final Logger log
    = Logger.getLogger(FileOutput.class.getName());

  private Env _env;
  private Path _path;
  private WriteStream _os;
  private long _offset;
  private FileLock _fileLock;
  private RandomAccessFile _randomAccessFile;

  public FileOutput(Env env, Path path)
    throws IOException
  {
    this(env, path, false);
  }

  public FileOutput(Env env, Path path, boolean isAppend)
    throws IOException
  {
    _env = env;
    
    env.addClose(this);
    
    _path = path;

    if (isAppend)
      _os = path.openAppend();
    else
      _os = path.openWrite();
  }

  /**
   * Returns the write stream.
   */
  public OutputStream getOutputStream()
  {
    return _os;
  }

  /**
   * Returns the file's path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Prints a string to a file.
   */
  public void print(char v)
    throws IOException
  {
    if (_os != null)
      _os.print(v);
  }

  /**
   * Prints a string to a file.
   */
  public void print(String v)
    throws IOException
  {
    if (_os != null)
      _os.print(v);
  }

  /**
   * Writes a character
   */
  public void write(int ch)
    throws IOException
  {
    if (_os != null)
      _os.write(ch);
  }

  /**
   * Writes a buffer to a file.
   */
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_os != null)
      _os.write(buffer, offset, length);
  }

  /**
   * Flushes the output.
   */
  public void flush()
    throws IOException
  {
    if (_os != null)
      _os.flush();
  }


  /**
   * Closes the file.
   */
  public void closeWrite()
  {
    close();
  }
  
  /**
   * Closes the file.
   */
  public void close()
  {
    try {
      _env.removeClose(this);
      
      WriteStream os = _os;
      _os = null;

      if (os != null)
        os.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      FileLock lock = _fileLock;
      _fileLock = null;
      
      if (lock != null)
        lock.release();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      RandomAccessFile file = _randomAccessFile;
      _randomAccessFile = null;
      
      if (file != null)
        file.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Lock the shared advisory lock.
   */
  public boolean lock(boolean shared, boolean block)
  {
    if (! (getPath() instanceof FilePath))
      return true;

    try {
      File file = ((FilePath) getPath()).getFile();

      if (_randomAccessFile == null) {
        _randomAccessFile = new RandomAccessFile(file, "rw");
      }

      FileChannel fileChannel = _randomAccessFile.getChannel();
      
      if (block)
        _fileLock = fileChannel.lock(0, Long.MAX_VALUE, shared);
      else 
        _fileLock = fileChannel.tryLock(0, Long.MAX_VALUE, shared);

      return _fileLock != null;
    } catch (OverlappingFileLockException e) {
      return false;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Unlock the advisory lock.
   */
  public boolean unlock()
  {
    try {
      FileLock lock = _fileLock;
      _fileLock = null;
      
      if (lock != null) {
        lock.release();

        return true;
      }
      else
	return true;
    } catch (IOException e) {
      return false;
    }
  }

  public Value stat()
  {
    return FileModule.statImpl(_env, getPath());
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "FileOutput[" + getPath() + "]";
  }
}


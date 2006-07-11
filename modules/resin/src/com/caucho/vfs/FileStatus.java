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
 * @author Emil Ong
 */

package com.caucho.vfs;

public class FileStatus {
  private static long S_IFMT = 00170000;
  private static long S_IFSOCK = 0140000;
  private static long S_IFLNK	= 0120000;
  private static long S_IFREG = 0100000;
  private static long S_IFBLK = 0060000;
  private static long S_IFDIR = 0040000;
  private static long S_IFCHR = 0020000;
  private static long S_IFIFO = 0010000;

  private long st_dev;
  private long st_ino;
  private int st_mode;
  private int st_nlink;
  private int st_uid;
  private int st_gid;
  private long st_rdev;
  private long st_size;
  private long st_blksize;
  private long st_blocks;
  private long st_atime;
  private long st_mtime;
  private long st_ctime;

  private boolean isRegularFile;
  private boolean isDirectory;
  private boolean isCharacterDevice;
  private boolean isBlockDevice;
  private boolean isFIFO;
  private boolean isLink;
  private boolean isSocket;

  public FileStatus(long st_dev, long st_ino, int st_mode, int st_nlink,
                    int st_uid, int st_gid, long st_rdev, long st_size, 
                    long st_blksize, long st_blocks, 
                    long st_atime, long st_mtime, long st_ctime,
                    boolean isRegularFile, boolean isDirectory,
                    boolean isCharacterDevice, boolean isBlockDevice,
                    boolean isFIFO, boolean isLink, boolean isSocket)
  {
    this.st_dev = st_dev;
    this.st_ino = st_ino;
    this.st_mode = st_mode;
    this.st_nlink = st_nlink;
    this.st_uid = st_uid;
    this.st_gid = st_gid;
    this.st_rdev = st_rdev;
    this.st_size = st_size;
    this.st_blksize = st_blksize;
    this.st_blocks = st_blocks;
    this.st_atime = st_atime;
    this.st_mtime = st_mtime;
    this.st_ctime = st_ctime;

    this.isRegularFile = isRegularFile;
    this.isDirectory = isDirectory;
    this.isCharacterDevice = isCharacterDevice;
    this.isBlockDevice = isBlockDevice;
    this.isFIFO = isFIFO;
    this.isLink = isLink;
    this.isSocket = isSocket;
  }

  public long getDev()
  {
    return st_dev;
  }

  public long getIno()
  {
    return st_ino;
  }

  public int getMode()
  {
    return st_mode;
  }

  public int getNlink()
  {
    return st_nlink;
  }

  public int getUid()
  {
    return st_uid;
  }

  public int getGid()
  {
    return st_gid;
  }

  public long getRdev()
  {
    return st_rdev;
  }

  public long getSize()
  {
    return st_size;
  }

  public long getBlksize()
  {
    return st_blksize;
  }

  public long getBlocks()
  {
    return st_blocks;
  }

  public long getAtime()
  {
    return st_atime;
  }

  public long getMtime()
  {
    return st_mtime;
  }

  public long getCtime()
  {
    return st_ctime;
  }

  public boolean isRegularFile() 
  {
    return isRegularFile;
  }

  public boolean isDirectory() 
  {
    return isDirectory;
  }

  public boolean isCharacterDevice() 
  {
    return isCharacterDevice;
  }

  public boolean isBlockDevice() 
  {
    return isBlockDevice;
  }

  public boolean isFIFO() 
  {
    return isFIFO;
  }

  public boolean isLink() 
  {
    return isLink;
  }

  public boolean isSocket() 
  {
    return isSocket;
  }
}

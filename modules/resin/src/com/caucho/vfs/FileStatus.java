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

  public FileStatus(Path path)
  {
    st_dev = 0;
    st_ino = 0;
    st_mode = 0;

    if (path.isDirectory())
      st_mode |= 0040111; // S_IFDIR
    else
      st_mode |= 0100000; // S_IFREG
    
    if (path.canRead())
      st_mode |= 0444;
    if (path.canWrite())
      st_mode |= 0220;
    if (path.canExecute())
      st_mode |= 0110;

    st_nlink = 1;
    st_uid = 501;
    st_gid = 501;
    st_rdev = 0;
    st_size = path.getLength();
    st_blksize = 4096;
    st_blocks = (path.getLength() + 4095) / 4096;

    st_atime = st_mtime = st_ctime = path.getLastModified() / 1000;
  }

  public FileStatus(long dev, long ino, int mode, int nlink,
                    int uid, int gid, long rdev, long size, long blksize,
                    long blocks, long atime, long mtime, long ctime)
  {
    st_dev = dev;
    st_ino = ino;
    st_mode = mode;
    st_nlink = nlink;
    st_uid = uid;
    st_gid = gid;
    st_rdev = rdev;
    st_size = size;
    st_blksize = blksize;
    st_blocks = blocks;
    st_atime = atime;
    st_mtime = mtime;
    st_ctime = ctime;
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
}

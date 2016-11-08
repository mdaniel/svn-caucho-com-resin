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

package com.caucho.env.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.caucho.java.WorkDir;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.Jar.ZipStreamImpl;

/**
 * Tree structure from a jar
 */
public class GitCommitJar {
  private static final L10N L = new L10N(GitCommitJar.class);
  
  private static final Logger log
    = Logger.getLogger(GitCommitJar.class.getName());
  
  private GitCommitTree _commit = new GitCommitTree();

  private JarPath _jar;
  
  private Path _tempJar;

  public GitCommitJar(Path jar)
    throws IOException
  {
    if (jar.getScheme().equals("memory")) {
      InputStream is = jar.openRead();
      
      try {
        init(is);
      } finally {
        is.close();
      }
    }
    else {
      init(jar);
    }
  }

  private GitCommitJar(Path jar, boolean isTemp)
    throws IOException
  {
    init(jar);
    
    _tempJar = jar;
  }

  public GitCommitJar(InputStream is)
    throws IOException
  {
    init(is);
  }
  
  public static GitCommitJar createDirectory(Path dir)
    throws IOException
  {
    if (! dir.isDirectory())
      throw new IOException(L.l("'{0}' must be a directory", dir));
    
    Path workDir = WorkDir.getLocalWorkDir();
    workDir.mkdirs();
    
    Path tmpPath = workDir.createTempFile("git", ".jar");
    WriteStream os = null;
    
    try {
      os = tmpPath.openWrite();
      
      ZipOutputStream zos = new ZipOutputStream(os);
      
      int count = fillDirectory(zos, dir, "");
      
      if (count > 0) {
        zos.close();
      } else {
        try {
          zos.close();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
  
      os.close();
      
      GitCommitJar gitCommitJar = new GitCommitJar(tmpPath, true);
      
      tmpPath = null;
      
      return gitCommitJar;
    } finally {
      IoUtil.close(os);
      
      if (tmpPath != null)
        tmpPath.remove();
    }
  }
  
  private static int fillDirectory(ZipOutputStream zos, 
                                   Path dir, 
                                   String pathName)
    throws IOException
  {
    int count = 0;
    
    String []list = dir.list();
    Arrays.sort(list);
    
    for (String name : list) {
      if (name.startsWith("."))
        continue;
      
      String subPath;
      
      if ("".equals(pathName))
        subPath = name;
      else
        subPath = pathName + "/" + name;
      
      Path path = dir.lookup(name);
     
      if (path.isFile()) {
        ZipEntry entry = new ZipEntry(subPath);

        zos.putNextEntry(entry);
        
        path.writeToStream(zos);
        
        zos.closeEntry();
        
        count++;
      }
      else if (path.isDirectory()) {
        int subcount = fillDirectory(zos, path, subPath);
        
        count += subcount;
      }
    }
    
    return count;
  }
  
  private void init(InputStream is)
    throws IOException
  {
    Path dir = WorkDir.getLocalWorkDir();
    dir.mkdirs();
    
    Path path = dir.createTempFile("git", "tmp");

    try {
      WriteStream os = path.openWrite();
      os.writeStream(is);
      os.close();

      init(path);

      _tempJar = path;
    } catch (IOException e) {
      path.remove();
    }
  }

  private void init(Path path)
    throws IOException
  {
    _jar = JarPath.create(path);

    /*
    HashMap<String,Long> lengthMap = new HashMap<String,Long>();

    fillLengthMap(lengthMap, path);

    ReadStream is = path.openRead();

    fillCommit(lengthMap, is);
    */
    
    //long len = path.getLength();
    
    ReadStream is = null;
    
    try {
      is = path.openRead();
      
      _commit.addFile(path.getTail(), 0664, is, path.getLength());
    } finally {
      IoUtil.close(is);
    }

    _commit.commit();
  }

  private void fillCommit(HashMap<String,Long> lengthMap, InputStream is)
    throws IOException
  {
    ZipInputStream zin = null;
    
    try {
      zin = new ZipInputStream(is);

      ZipEntry entry;
      
      while ((entry = zin.getNextEntry()) != null) {
        String path = entry.getName();
        long length = entry.getSize();

        if (entry.isDirectory())
          continue;

        Long lengthValue = lengthMap.get(path);

        if (lengthValue != null)
          length = lengthValue;

        _commit.addFile(path, 0664, zin, length);
      }
    } finally {
      IoUtil.close(zin);
      is.close();
    }
  }

  public String []getCommitList()
  {
    return _commit.getCommitList();
  }

  public String getDigest()
  {
    return _commit.getDigest();
  }

  public String findPath(String sha1)
  {
    return _commit.findPath(sha1);
  }

  private void fillLengthMap(HashMap<String,Long> lengthMap, Path jar)
    throws IOException
  {
    ReadStream is = jar.openRead();
    ZipInputStream zin = null;
    
    try {
      zin = new ZipInputStream(is);

      ZipEntry entry;
      
      while ((entry = zin.getNextEntry()) != null) {
        String path = entry.getName();
        long length = entry.getSize();

        if (entry.isDirectory())
          continue;

        if (length < 0) {
          length = 0;

          while (zin.read() >= 0) {
            length++;
          }
        }

        lengthMap.put(path, length);
      }
    } finally {
      IoUtil.close(zin);
      
      IoUtil.close(is);
    }
  }

  private long getLength(String path)
    throws IOException
  {
    InputStream is = null;

    try {
      ZipStreamImpl zipIs = _jar.getJar().openReadImpl(path); 
      is = new ReadStream(zipIs);
    
      long length = 0;

      while (is.read() >= 0) {
        length++;
      }

      return length;
    } finally {
      IoUtil.close(is);
    }
  }

  public InputStream openFile(String sha1)
    throws IOException
  {
    String path = _commit.findPath(sha1);

    if (path.endsWith("/")) {
      GitWorkingTree tree = _commit.findTree(path);

      return tree.openFile();
    }
    else {
      Path jar = _jar.getContainer();
      
      ReadStream is = null;
      
      try {
        is = jar.openRead();
        
        return GitCommitTree.writeBlob(is, jar.getLength());
      } finally {
        IoUtil.close(is);;
      }
      /*
      long size = _jar.getJar().getLength(path);
      
      if (size < 0)
        size = getLength(path);
      
      ZipStreamImpl zipIs = _jar.getJar().openReadImpl(path);
      
      ReadStream is = new ReadStream(zipIs);
      
      try {
        return GitCommitTree.writeBlob(is, size);
      } finally {
        is.close();
        
        zipIs.close();
      }
      */
    }
  }

  public void close()
  {
    if (_jar != null)
      _jar.closeJar();

    if (_tempJar != null) {
      try {
        _tempJar.remove();
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }
}

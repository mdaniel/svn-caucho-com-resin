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

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import com.caucho.env.service.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Top-level class for a repository
 */
public class GitSystem extends AbstractResinSubSystem 
{
  public static final int START_PRIORITY
  = RootDirectorySystem.START_PRIORITY_ROOT_DIRECTORY + 1; 

  private static final L10N L = new L10N(GitSystem.class);
  private static final Logger log = 
    Logger.getLogger(GitSystem.class.getName());
  
  private Path _root;
  
  public GitSystem(Path root)
  {
    _root = root;
  }
  
  public static GitSystem createAndAddService()
  {
    return createAndAddService(null);
  }

  public static GitSystem createAndAddService(Path root)
  {
    ResinSystem system = preCreate(GitSystem.class);
    
    GitSystem service = new GitSystem(root);
    system.addService(GitSystem.class, service);
    
    return service;
  }

  public static GitSystem getCurrent()
  {
    return ResinSystem.getCurrentService(GitSystem.class);
  }

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  @Override
  public void start()
    throws IOException
  {
    if (_root == null)
      _root = RootDirectorySystem.getCurrentDataDirectory().lookup(".git");

    if (_root.lookup("HEAD").canRead())
      return;

    _root.mkdirs();

    _root.lookup("refs").mkdir();
    _root.lookup("refs/heads").mkdir();
    
    _root.lookup("objects").mkdir();
    _root.lookup("objects/info").mkdir();
    _root.lookup("objects/pack").mkdir();
    
    _root.lookup("branches").mkdir();
    
    _root.lookup("tmp").mkdir();

    WriteStream out = _root.lookup("HEAD").openWrite();
    try {
      out.println("ref: refs/heads/master");
    } finally {
      out.close();
    }
  }

  public String getMaster()
  {
    return getTag("heads/master");
  }

  /**
   * Returns the object type of the specified file.
   *
   * @param sha1 the sha1 hash identifier of the file
   *
   * @return "blob", "commit" or "tree"
   */
  public GitType objectType(String sha1)
    throws IOException
  {
    GitObjectStream is = open(sha1);

    try {
      return is.getType();
    } finally {
      is.close();
    }
  }
  
  public String getTag(String tag)
  {
    Path path = getRefPath(tag);

    synchronized (this) {
      if (! path.canRead())
        return null;

      ReadStream is = null;
      try {
        is = path.openRead();

        String hex = is.readLine();

        if (hex != null)
          return hex.trim();
        else
          return null;
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      } finally {
        if (is != null)
          is.close();
      }
    }
  }

  public void writeTag(String tag, String hex)
  {
    Path path = getRefPath(tag);

    try {
      path.getParent().mkdirs();
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    synchronized (this) {
      WriteStream out = null;
      try {
        out = path.openWrite();

        out.println(hex);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        try {
          if (out != null)
            out.close();
        } catch (Exception e) {
          log.log(Level.FINEST, e.toString(), e);
        }
      }
    }
  }

  public String []listRefs(String dir)
  {
    try {
      Path path = getRefPath(dir);

      if (path.isDirectory())
        return path.list();
      else
        return new String[0];
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return new String[0];
    }
  }

  private Path getRefPath(String path)
  {
    return _root.lookup("refs").lookup(path);
  }

  /**
   * Parses and returns the commit file specified by the sha1 hash.
   *
   * @param sha1 the sha1 hash identifier of the commit file
   *
   * @return the parsed GitCommit structure
   */
  public GitCommit parseCommit(String sha1)
    throws IOException
  {
    GitObjectStream is = open(sha1);
    try {
      if (is.getType() != GitType.COMMIT)
        throw new IOException(L.l("'{0}' is an unexpected type, expected 'commit'",
                                  is.getType()));
      
      return is.parseCommit();
    } finally {
      is.close();
    }
  }

  /**
   * Parses and returns the tree (directory) specified by the sha1 hash.
   *
   * @param sha1 the sha1 hash identifier of the tree file
   *
   * @return the parsed GitTree structure
   */
  public GitTree parseTree(String sha1)
    throws IOException
  {
    GitObjectStream is = open(sha1);
    
    try {
      if (GitType.TREE != is.getType())
        throw new IOException(L.l("'{0}' is an unexpected type, expected 'tree'",
                                  is.getType()));
      
      return is.parseTree();
    } finally {
      is.close();
    }
  }

  /**
   * Returns an input stream to a blob
   */
  public InputStream openBlob(String sha1)
    throws IOException
  {
    GitObjectStream is = open(sha1);
    
    if (is.getType() != GitType.BLOB) {
      is.close();
      throw new IOException(L.l("'{0}' is an unexpected type, expected 'blob'",
                                is.getType()));
    }
      
    return is;
  }

  public void expandToPath(Path path, String sha1)
    throws IOException
  {
    long now = CurrentTime.getCurrentTime();
    
    expandToPath(path, sha1, now);
  }
  
  public void expandToPath(Path path, String sha1, long now)
    throws IOException
  {
    GitObjectStream is = open(sha1);

    try {
      if (GitType.TREE == is.getType()) {
        GitTree tree = is.parseTree();
        is.close();

        expandTreeToPath(path, tree, now);
      }
      else if (GitType.BLOB == is.getType()) {
        if (path.canRead() && path.getLength() == is.getLength()) {
          String pathSha1 = getBlobSha1(path);
          
          if (sha1.equals(pathSha1))
            return;
        }
        
        if (path.getTail().endsWith(".war")) {
          ZipInputStream zis = new ZipInputStream(is);
          
          try {
            expandZipToPath(zis, path.getParent());
          } finally {
            zis.close();
          }
          return;
        }
        
        WriteStream os = path.openWrite();

        try {
          os.writeStream(is.getInputStream());
        } finally {
          os.close();
        }

        // #3839
        path.setLastModified(now);
      }
      else
        throw new IOException(L.l("'{0}' is an unexpected type, expected 'blob' or 'tree'",
                                  is.getType()));
    } finally {
      is.close();
    }
  }
  
  private void expandZipToPath(ZipInputStream is, Path path)
    throws IOException
  {
    ZipEntry entry;
    
    while ((entry = is.getNextEntry()) != null) {
      String name = entry.getName();
      
      if (entry.isDirectory()) {
        path.lookup(name).mkdirs();
      }
      else {
        Path subPath = path.lookup(name);
        subPath.getParent().mkdirs();
        
        WriteStream os = subPath.openWrite();
        try {
          os.writeStream(is);
        } finally {
          os.close();
        }
      }
    }
  }

  private void expandTreeToPath(Path path, GitTree tree, long now)
    throws IOException
  {
    path.mkdirs();
    
    for (GitTree.Entry entry : tree.entries()) {
      String name = entry.getName();

      expandToPath(path.lookup(name), entry.getSha1(), now);
    }
  }

  public void copyToFile(Path path, String sha1)
    throws IOException
  {
    GitObjectStream is = open(sha1);
    
    try {
      if (GitType.BLOB != is.getType())
        throw new IOException(L.l("'{0}' is an unexpected type, expected 'blob'",
                                  is.getType()));

      WriteStream os = path.openWrite();

      try {
        os.writeStream(is.getInputStream());
      } finally {
        os.close();
      }
    } finally {
      is.close();
    }
  }

  public boolean contains(String hash)
  {
    String prefix = hash.substring(0, 2);
    String suffix = hash.substring(2);

    Path path = _root.lookup("objects").lookup(prefix).lookup(suffix);

    return path.exists();
  }

  /**
   * Opens an object file specified by a sha1 hash.
   *
   * @param sha1 the sha1 hash identifier for the file
   *
   * @return an opened GitObjectStream to the file
   */
  public GitObjectStream open(String sha1)
    throws IOException
  {
    String prefix = sha1.substring(0, 2);
    String suffix = sha1.substring(2);

    Path path = _root.lookup("objects").lookup(prefix).lookup(suffix);

    return new GitObjectStream(path);
  }

  /**
   * Writes a file to the repository
   */
  public String writeFile(Path path)
    throws IOException
  {
    InputStream is = path.openRead();
    
    try {
      TempOutputStream os = new TempOutputStream();
      String type = "blob";

      String hex = writeData(os, type, is, path.getLength());

      return writeFile(os, hex);
    } finally {
      is.close();
    }
  }

  /**
   * Writes a file to the repository
   */
  public String getBlobSha1(Path path)
    throws IOException
  {
    InputStream is = path.openRead();
    
    try {
      NullOutputStream os = new NullOutputStream();
      String type = "blob";

      String hex = writeData(os, type, is, path.getLength());

      return hex;
    } finally {
      is.close();
    }
  }

  /**
   * Writes a file to the repository
   */
  public String writeInputStream(InputStream is)
    throws IOException
  {
    TempStream tempOs = new TempStream();

    WriteStream out = new WriteStream(tempOs);

    out.writeStream(is);

    out.close();

    int length = tempOs.getLength();

    String type = "blob";

    TempOutputStream os = new TempOutputStream();

    String sha1 = writeData(os, type, tempOs.getInputStream(), length);

    return writeFile(os, sha1);
  }

  /**
   * Writes a file to the repository
   */
  public String writeInputStream(InputStream is, long length)
    throws IOException
  {
    String type = "blob";

    TempOutputStream os = new TempOutputStream();

    String sha1 = writeData(os, type, is, length);

    return writeFile(os, sha1);
  }

  /**
   * Writes a file to the repository
   */
  public String writeTree(GitTree tree)
    throws IOException
  {
    TempOutputStream treeOut = new TempOutputStream();

    tree.toData(treeOut);
    
    int treeLength = treeOut.getLength();
    
    InputStream is = treeOut.openRead();
    
    try {
      TempOutputStream os = new TempOutputStream();
      String type = "tree";

      String hex = writeData(os, type, is, treeLength);

      return writeFile(os, hex);
    } finally {
      is.close();
    }
  }

  /**
   * Writes a file to the repository
   */
  public String writeCommit(GitCommit commit)
    throws IOException
  {
    TempStream commitOut = new TempStream();
    WriteStream out = new WriteStream(commitOut);

    out.print("tree ");
    out.println(commit.getTree());

    String parent = commit.getParent();

    if (parent != null) {
      out.print("parent ");
      out.println(parent);
    }

    Map<String,String> attr = commit.getMetaData();
    if (attr != null) {
      ArrayList<String> keys = new ArrayList<String>(attr.keySet());
      Collections.sort(keys);

      for (String key : keys) {
        out.print(key);
        out.print(' ');
        out.print(attr.get(key));
        out.println();
      }
    }

    out.println();

    if (commit.getMessage() != null)
      out.println(commit.getMessage());
    
    out.close();
    
    int commitLength = commitOut.getLength();
    
    InputStream is = commitOut.openRead();
    
    try {
      TempOutputStream os = new TempOutputStream();
      String type = "commit";

      String hex = writeData(os, type, is, commitLength);

      return writeFile(os, hex);
    } finally {
      is.close();
    }
  }

  public String writeFile(TempOutputStream os, String hex)
    throws IOException
  {
    Path objectPath = lookupPath(hex);

    if (objectPath.exists())
      return hex;

    objectPath.getParent().mkdirs();
    
    Path tmpDir = _root.lookup("tmp");
    tmpDir.mkdirs();

    Path tmp = _root.lookup("tmp").lookup("tmp." + hex);

    WriteStream tmpOs = tmp.openWrite();
    try {
      tmpOs.writeStream(os.openRead());
    } finally {
      tmpOs.close();
    }

    tmp.renameTo(objectPath);
      
    return hex;
  }

  /**
   * Opens a stream to the raw git file.
   */
  public InputStream openRawGitFile(String sha1)
    throws IOException
  {
    Path objectPath = lookupPath(sha1);

    return objectPath.openRead();
  }

  /**
   * Writes a raw git file directly to the repository with an expected
   * sha1.  The write will verify that the stream matches the expected
   * content.
   */
  public String writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    Path objectPath = lookupPath(sha1);

    if (objectPath.exists())
      return sha1;

    objectPath.getParent().mkdirs();
    
    Path tmpDir = _root.lookup("tmp");
    tmpDir.mkdirs();

    Path tmp = _root.lookup("tmp").lookup("tmp." + sha1);

    try {
      WriteStream tmpOut = tmp.openWrite();
      
      try {
        tmpOut.writeStream(is);
      } finally {
        tmpOut.close();
      }

      String newHex = validate(tmp);

      if (! sha1.equals(newHex))
        throw new RuntimeException(L.l("{0}: file validation failed because sha-1 hash '{0}' does not match expected '{1}'",
                                       newHex, sha1));

      tmp.renameTo(objectPath);
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " addRawGitFile " + sha1 + " " + objectPath);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      tmp.remove();
    }
      
    return sha1;
  }

  /**
   * Validate and remove.
   */
  public void validateRawGitFile(String sha1)
  {
    Path objectPath = lookupPath(sha1);

    if (! objectPath.exists())
      return;
    
    boolean isValid = false;

    try {
      String newHex = validate(objectPath);

      if (sha1.equals(newHex))
        isValid = true;
      else {
        log.warning(L.l("{0}: file validation failed because sha-1 hash '{0}' does not match expected '{1}'",
                        newHex, sha1));
      }
    } catch (Exception e) {
      log.warning("git service " + sha1 + " " + e.toString());
    } finally {
      if (! isValid) {
        try {
          objectPath.remove();
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }
  }
  
  private Path lookupPath(String sha1)
  {
    String prefix = sha1.substring(0, 2);
    String suffix = sha1.substring(2);
    
    return _root.lookup("objects").lookup(prefix).lookup(suffix);
  }
  
  public static GitType validate(String hash, InputStream is)
    throws IOException, NoSuchAlgorithmException
  {
    MessageDigest md = MessageDigest.getInstance("SHA-1");

    InflaterInputStream zin = new InflaterInputStream(is);
    DigestInputStream din = new DigestInputStream(zin, md);

    TempBuffer tBuf = TempBuffer.allocate();
    byte []buffer = tBuf.getBuffer();
    
    GitType gitType = null;
    int len;

    while ((len = din.read(buffer, 0, buffer.length)) >= 0) {
      if (gitType == null) {
        String value = new String(buffer, 0, len);
        
        if (value.startsWith("blob"))
          gitType = GitType.BLOB;
        else if (value.startsWith("tree"))
          gitType = GitType.TREE;
        else if (value.startsWith("commit"))
          gitType = GitType.COMMIT;
      }
      
    }

    TempBuffer.free(tBuf);

    din.close();

    byte []digest = md.digest();
    
    String digestHash = Hex.toHex(digest);
    
    if (! hash.equals(digestHash))
      throw new IOException(L.l("Git file corrupted.\n  expected: {0}\n actual: {1}",
                                hash, digestHash));

    
    return gitType;
  }

  private String validate(Path path)
    throws IOException, NoSuchAlgorithmException
  {
    ReadStream is = path.openRead();

    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      
      InflaterInputStream zin = new InflaterInputStream(is);
      DigestInputStream din = new DigestInputStream(zin, md);

      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.getBuffer();
      
      while (din.read(buffer, 0, buffer.length) >= 0) {
      }
      
      TempBuffer.free(tBuf);

      din.close();

      byte []digest = md.digest();

      return Hex.toHex(digest);
    } finally {
      is.close();
    }
  }

  public static String writeData(OutputStream os, String type,
                                 InputStream is, long length)
    throws IOException
  {
    TempBuffer buf = TempBuffer.allocate();

    try {
      // DeflaterOutputStream out = new DeflaterOutputStream(os);
      ResinDeflaterOutputStream out = new ResinDeflaterOutputStream(os);

      MessageDigest md = MessageDigest.getInstance("SHA-1");

      for (int i = 0; i < type.length(); i++) {
        int ch = type.charAt(i);
        out.write(ch);
        md.update((byte) ch);
      }

      out.write(' ');
      md.update((byte) ' ');
      
      String lengthString = String.valueOf(length);

      for (int i = 0; i < lengthString.length(); i++) {
        int ch = lengthString.charAt(i);
        out.write(ch);
        md.update((byte) ch);
      }
      
      out.write(0);
      md.update((byte) 0);

      long readLength = 0;
      
      int len;

      byte []buffer = buf.getBuffer();
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        out.write(buffer, 0, len);
        md.update(buffer, 0, len);

        readLength += len;
      }

      out.close();

      if (readLength != length)
        throw new IOException(L.l("written length does not match data"));

      return Hex.toHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } finally {
      TempBuffer.free(buf);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _root + "]";
  }
}

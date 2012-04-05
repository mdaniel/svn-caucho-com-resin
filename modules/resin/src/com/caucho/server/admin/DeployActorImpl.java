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

package com.caucho.server.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.deploy.CopyTagQuery;
import com.caucho.cloud.deploy.RemoveTagQuery;
import com.caucho.cloud.deploy.SetTagQuery;
import com.caucho.config.ConfigException;
import com.caucho.env.deploy.DeployControllerService;
import com.caucho.env.deploy.DeployException;
import com.caucho.env.deploy.DeployTagItem;
import com.caucho.env.git.GitTree;
import com.caucho.env.repository.RepositorySpi;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.management.server.DeployControllerMXBean;
import com.caucho.management.server.EAppMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.host.HostController;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.Vfs;

public class DeployActorImpl
{
  public static final String ADDRESS = "deploy-proxy@resin.caucho";

  private static final Logger log
    = Logger.getLogger(DeployActorImpl.class.getName());

  private static final L10N L = new L10N(DeployActorImpl.class);

  private final RepositorySpi _repository;

  DeployActorImpl(RepositorySpi repository)
  {
    _repository = repository;
  }

  /*
  public DeployGetFileQuery getFile(String tag, String fileName)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " getFile " + tag + " " + fileName);
    
    while (fileName.startsWith("/")) {
      fileName = fileName.substring(1);
    }
    
    RepositoryTagEntry entry = _repository.getTagMap().get(tag);
    
    if (entry == null) {
      throw new ConfigException(L.l("'{0}' is an unknown repository tag",
                                    tag));
    }
    
    String sha1 = entry.getRoot();
    
    String fileSha = findFile(sha1, fileName, fileName);

    BlobStreamSource iss = new BlobStreamSource(_repository, fileSha);
    
    StreamSource source = new StreamSource(iss);
    
    return new DeployGetFileQuery(tag, sha1, source);
  }
  */
  
  private String findFile(String sha1, String fullFilename, String fileName)
    throws IOException
  {
    if (fileName.equals("")) {
      if (_repository.isBlob(sha1))
        return sha1;
      else {
        throw new ConfigException(L.l("'{0}' is not a file", fullFilename));
      }
    }
    
    int p = fileName.indexOf('/');
    String tail = "";
    
    if (p > 0) {
      tail = fileName.substring(p + 1);
      fileName = fileName.substring(0, p);
    }
    
    if (! _repository.isTree(sha1))
      throw new ConfigException(L.l("'{0}' is an invalid path", fullFilename));
    
    GitTree tree = _repository.readTree(sha1);
    
    String childSha1 = tree.getHash(fileName);
    
    if (childSha1 == null)
      throw new ConfigException(L.l("'{0}' is an unknown file",
                                    fullFilename));
   
    return findFile(childSha1, fullFilename, tail);
  }

  public String []getFileList(String tag, String fileName)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " getFileList " + tag + " " + fileName);
    
    RepositoryTagEntry entry = _repository.getTagMap().get(tag);
    
    if (entry == null) {
      throw new ConfigException(L.l("'{0}' is an unknown repository tag",
                                    tag));
    }
    
    String sha1 = entry.getRoot();
    
    ArrayList<String> fileList = new ArrayList<String>();
    
    listFiles(fileList, sha1, "");
    
    Collections.sort(fileList);
    
    String []files = new String[fileList.size()];
    
    fileList.toArray(files);
    
    return files;
  }
  
  private void listFiles(ArrayList<String> files, 
                         String sha1,
                         String prefix)
    throws IOException
  {
    if (sha1 == null)
      return;
    
    if (_repository.isBlob(sha1)) {
      files.add(prefix);
      return;
    }
    
    if (! _repository.isTree(sha1))
      throw new ConfigException(L.l("'{0}' is an invalid path", prefix));
    
    GitTree tree = _repository.readTree(sha1);
    
    for (String key : tree.getMap().keySet()) {
      String name;
      
      if ("".equals(prefix))
        name = key;
      else
        name = prefix + "/" + key;
      
      listFiles(files, tree.getHash(key), name);
    }
  }
  
  static class BlobStreamSource extends StreamSource {
    private RepositorySpi _repository;
    private String _sha1;
    
    BlobStreamSource(RepositorySpi repository, String sha1)
    {
      _repository = repository;
      _sha1 = sha1;
    }
    
    /**
     * Returns an input stream, freeing the results
     */
    @Override
    public InputStream getInputStream()
      throws IOException
    {
      return _repository.openRawGitFile(_sha1);
    }

    /**
     * Returns an input stream, without freeing the results
     */
    @Override
    public InputStream openInputStream()
      throws IOException
    {
      return _repository.openRawGitFile(_sha1);
    }
  }
}

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
 * @author Scott Ferguson
 */

package com.caucho.v5.http.pod;

import io.baratine.files.BfsFileSync;
import io.baratine.files.WriteOption;
import io.baratine.service.ResultFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.baratine.ServicePod;
import com.caucho.v5.baratine.ServiceServer;
import com.caucho.v5.baratine.ServiceServer.PodBuilder;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.bartender.pod.UpdatePod;
import com.caucho.v5.bartender.pod.PodBartender.PodType;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Contains Baratine service pod deployments.
 */
class PodBuilderImpl implements ServiceServer.PodBuilder
{
  private static final Logger log
    = Logger.getLogger(PodBuilderImpl.class.getName());
  
  private transient final ServiceServer _server;
  private transient final PodBuilderServiceSync _podBuilder;
  private transient ServiceManagerAmp _manager;
  
  private final String _podName;
  private int _size = -1;
  private PodType _type = PodBartender.PodType.lazy;
  
  private ArrayList<ServiceServer.Server> _servers = new ArrayList<>();
  private ArrayList<PathImpl> _pathList = new ArrayList<>();
  
  private ArrayList<PathImpl> _classPathList = new ArrayList<>();
  
  private ArrayList<String> _applicationPaths = new ArrayList<>();
  
  private int _journalMaxCount = -1;
  private long _journalTimeout = -1;

  PodBuilderImpl(ServiceServer server,
                 ServiceManagerAmp manager,
                 PodBuilderServiceSync podBuilder,
                 String podName)
  {
    // Objects.requireNonNull(server);
    Objects.requireNonNull(manager);
    Objects.requireNonNull(podBuilder);
    Objects.requireNonNull(podName);

    _server = server;
    _manager = manager;
    _podBuilder = podBuilder;
    _podName = podName;
  }

  PodBuilderImpl(String podName)
  {
    _server = null;
    _podBuilder = null;
    _podName = podName;
  }

  @Override
  public String getName()
  {
    return _podName;
  }
  
  @Override
  public PodBuilderImpl size(int size)
  {
    _size = size;
    
    return this;
  }
  
  @Override
  public int getSize()
  {
    if (_size >= 0) {
      return _size;
    }
    else if (_type == null) {
      return 1;
    }
    else {
      switch (_type) {
      case off:
        return 0;
      case solo:
      case lazy:
        return 1;
      case pair:
        return 2;
      case triad:
        return 3;
      case cluster:
        return 4;
      default:
        throw new IllegalStateException(String.valueOf(_type));
      }
    }
  }
  
  public PodBartender.PodType getType()
  {
    return _type;
  }
  
  public PodBuilder type(PodType type)
  {
    Objects.requireNonNull(type);
    
    _type = type;
    
    return this;
  }
  
  @Override
  public PodBuilder off()
  {
    _type = PodBartender.PodType.off;
    
    return this;
  }
  
  @Override
  public PodBuilder solo()
  {
    _type = PodBartender.PodType.solo;
    
    return this;
  }
  
  @Override
  public PodBuilder pair()
  {
    _type = PodBartender.PodType.pair;
    
    return this;
  }
  
  @Override
  public PodBuilder triad()
  {
    _type = PodBartender.PodType.triad;
    
    return this;
  }
  
  @Override
  public PodBuilder cluster()
  {
    _type = PodBartender.PodType.cluster;
    
    return this;
  }
  
  public Iterable<ServiceServer.Server> getServers()
  {
    return _servers;
  }
  
  @Override
  public PodBuilder server(String address, int port)
  {
    _servers.add(new ServerImpl(address, port));
    
    return this;
  }
  
  @Override
  public PodBuilder journalMaxCount(int count)
  {
    _journalMaxCount = count;
    
    return this;
  }
  
  @Override
  public int getJournalMaxCount()
  {
    return _journalMaxCount;
  }
  
  @Override
  public long getJournalDelay()
  {
    return _journalTimeout;
  }
  
  @Override
  public PodBuilder journalDelay(long timeout, TimeUnit unit)
  {
    _journalTimeout = unit.toMillis(timeout);
    
    return this;
  }
  
  @Override
  public PodBuilder deploy(String pathName)
  {
    PathImpl path = VfsOld.lookup(pathName);
    
    _pathList.add(path);
    
    return this;
  }
  
  @Override
  public PodBuilder classPath(PathImpl path)
  {
    // Path path = Vfs.lookup(pathName);
    
    _classPathList.add(path);
    
    return this;
  }
  
  @Override
  public Iterable<String> getApplicationPaths()
  {
    return _applicationPaths;
  }

  @Override
  public ServicePod build()
  {
    deployClassPath();
    
    deployBar();
    
    _podBuilder.buildPod(this);
    
    //PodBartender pod = _podDeploy.findPod(getName());
    
    //PodBartender pod = future.get(10, TimeUnit.SECONDS);

    ServicePod podProxy = _server.pod(getName());
      
    if (podProxy == null) {
      // XXX: timing issues, cloud/0b25
      try { Thread.sleep(1000); } catch (Exception e) {}
      
      podProxy = _server.pod(getName());
    }
      
    return podProxy;
  }
  
  /*
  UpdatePod buildUpdate()
  {
    return null;
  }
  */
  
  //
  // classpath deployment code
  //
  private void deployClassPath()
  {
    if (_classPathList.size() == 0) {
      return;
    }
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_manager.classLoader());
      
      PodContainer podContainer = PodContainer.getCurrent();
      
      for (PathImpl path : _classPathList) {
        podContainer.addPodClassPath(_podName, path);
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }
  
  //
  // BFS deployment code
  //
  
  private void deployBar()
  {
    if (_pathList.size() <= 0) {
      return;
    }
    
    String url = "bfs:///usr/local/pods/" + _podName + ".bar";
    
    _applicationPaths.add(url);

    BfsFileSync podFile = _manager.service(url).as(BfsFileSync.class);
    
    try (OutputStream os = podFile.openWrite(WriteOption.Standard.CLOSE_WAIT_FOR_PUT)) {
      if (_pathList.size() == 1 && _pathList.get(0).getTail().endsWith(".bar")) {
        writeToZipBar(os, _pathList.get(0));
        return;
      }
      
      try (ZipOutputStream out = new ZipOutputStream(os)) {
        for (PathImpl path : _pathList) {
          writeToZip(out, path);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void writeToZip(ZipOutputStream out,
                          PathImpl path)
    throws IOException
  {
    if (path.isDirectory()) {
      writeToZipDirectory(out, path, "");
    }
    else if (path.getTail().endsWith(".jar")) {
      writeToZipJar(out, path);
    }
    else {
      log.warning("Invalid path: " + path);
    }
  }
  
  private void writeToZipBar(OutputStream os, PathImpl path)
    throws IOException
  {
    path.writeToStream(os);
  }
  
  private void writeToZipJar(ZipOutputStream out, PathImpl path)
    throws IOException
  {
    String name = path.getTail();
    
    ZipEntry entry = new ZipEntry("lib/" + name);
    entry.setSize(path.length());
    
    out.putNextEntry(entry);
    
    path.writeToStream(out);
  }
  
  private void writeToZipDirectory(ZipOutputStream out, 
                                   PathImpl dir,
                                   String parentPath)
    throws IOException
  {
    if (! dir.isDirectory()) {
      return;
    }
    
    for (String fileName : dir.list()) {
      PathImpl path = dir.lookup(fileName);

      String name;

      if (parentPath.isEmpty()) {
        name = fileName;
      }
      else {
        name = parentPath + "/" + fileName;
      }
      
      if (path.isDirectory()) {
        writeToZipDirectory(out, path, name);
      }
      else {
        ZipEntry entry = new ZipEntry(name);
        entry.setSize(path.length());
        
        out.putNextEntry(entry);

        path.writeToStream(out);
      }
    }
  }
  
  private class ServerImpl implements ServiceServer.Server {
    private String _address;
    private int _port;
    
    ServerImpl(String address, int port)
    {
      _address = address;
      _port = port;
    }
    
    @Override
    public String getAddress()
    {
      return _address;
    }
    
    @Override
    public int getPort()
    {
      return _port;
    }
  }
}

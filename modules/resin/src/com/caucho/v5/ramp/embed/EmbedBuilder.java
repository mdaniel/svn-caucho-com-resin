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

package com.caucho.v5.ramp.embed;

import io.baratine.service.Service;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.remote.ServiceNodeBase;
import com.caucho.v5.amp.spi.ServiceBuilderAmp;
import com.caucho.v5.amp.spi.ServiceManagerBuilderAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.baratine.ServiceServer;
import com.caucho.v5.bytecode.scan.ScanClass;
import com.caucho.v5.bytecode.scan.ScanListenerByteCode;
import com.caucho.v5.bytecode.scan.ScanManagerByteCode;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.CharSegment;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.Vfs;


/**
 * Endpoint for receiving hamp message
 */
public class EmbedBuilder
{
  private static final Logger log = Logger.getLogger(EmbedBuilder.class.getName());
  
  private static final String SERVICE = Service.class.getName();
  
  private ArrayList<URL> _scanList = new ArrayList<>();
  
  private HashSet<String> _serviceSet = new HashSet<>();

  private ServiceManagerAmp _manager;

  private String _name;
  private PathImpl _root;
  private PathImpl _config;

  private ServiceServer _server;

  private PathImpl _data;

  private String _podName;

  /**
   * name assigns a debug name to the service manager 
   */
  public EmbedBuilder name(String name)
  {
    _name = name;
    
    return this;
  }

  /**
   * config specifies a configuration file
   */
  public EmbedBuilder config(String pathName)
  {
    _config = Vfs.lookup(pathName);
    
    return this;
  }

  public void podName(String podName)
  {
    _podName = podName;
  }
  
  public String getPodName()
  {
    return _podName;
  }
  
  public EmbedBuilder scan(String path)
  {
    try {
      URL url = new URL(path);
      
      return scan(url);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
  
  public EmbedBuilder scan(URL url)
  {
    Objects.requireNonNull(url);
    
    if (url.getPath().contains("baratine.jar")) {
      // XXX: need more sophistated
      return this;
    }
      
    _scanList.add(url);
    
    return this;
  }

  public EmbedBuilder root(String dataPath)
  {
    _root = Vfs.lookup(dataPath);
    
    return this;
  }

  public EmbedBuilder data(String dataPath)
  {
    _data = Vfs.lookup(dataPath);
    
    return this;
  }
  
  public EmbedBuilder scanClassLoader()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    if (! (loader instanceof URLClassLoader)) {
      return this;
    }
    
    @SuppressWarnings("resource")
    URLClassLoader urlLoader = (URLClassLoader) loader;

    URL []urls = urlLoader.getURLs();
      
    if (urls == null) {
      return this;
    }
    
    for (URL url : urls) {
      if (url != null) {
        scan(url);
      }
    }
    
    return this;
  }
  
  private void addService(String className)
  {
    _serviceSet.add(className);
  }
  
  public ServiceManagerAmp build()
  {
    if (ServiceManagerAmp.current() != null) {
      _manager = ServiceManagerAmp.current();
      
      scanImpl();
      
      return _manager;
    }
    else if (AmpSystem.getCurrent() != null) {
      return buildManager();
    }
    else {
      return buildServer();
    }
  }
  
  private ServiceManagerAmp buildManager()
  {
    ServiceManagerBuilderAmp builder = Amp.newManagerBuilder();
    
    if (_name != null) {
      builder.name(_name);
    }
    
    builder.setPodNode(new ServiceNodeBase(_podName));
    
    ServiceManagerAmp manager = builder.start();
    
    Amp.setContextManager(manager);

    _manager = manager;
    
    scanImpl();
    
    return manager;
  }
  
  private void scanImpl()
  {
    ScanListenerByteCode listener = new ScanListenerEmbed();
    
    ScanManagerByteCode scanManager = new ScanManagerByteCode(listener);
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    for (URL url : _scanList) {
      PathImpl root = Vfs.lookup(url.toString());
      
      scanManager.scan(loader, root, null);
    }

    for (String className : _serviceSet) {
      addService(_manager, className);
    }
  }
  
  private ServiceManagerAmp buildServer()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    ServiceServer.Builder builder = ServiceServer.newServer();
    
    /*
    if (_name != null) {
      builder.name(_name);
    }
    */
    
    if (_root != null) {
      builder.root(_root.getPath());
    }
    
    if (_data != null) {
      builder.data(_data.getPath());
    }
    
    if (_config != null) {
      builder.conf(_config.getURL());
    }
    
    if (_podName != null) {
      builder.podName(_podName);
    }
    
    _server = builder.build();
    
    _manager = (ServiceManagerAmp) _server.client();

    Objects.requireNonNull(_manager);
    
    Amp.setContextManager(_manager, loader);
    
    ScanListenerByteCode listener = new ScanListenerEmbed();
    
    ScanManagerByteCode scanManager = new ScanManagerByteCode(listener);
    
    // ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    for (URL url : _scanList) {
      PathImpl root = Vfs.lookup(url.toString());
      
      scanManager.scan(loader, root, null);
    }
    
    for (String className : _serviceSet) {
      addService(_manager, className);
    }
    
    return _manager;
  }
  
  private void addService(ServiceManagerAmp manager, String className)
  {
    Class<?> serviceClass;
    
    try {
      serviceClass = Class.forName(className, false, manager.getClassLoader());
    } catch (ClassNotFoundException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return;
    }
    
    Service service = serviceClass.getAnnotation(Service.class);
    
    ServiceBuilderAmp builder = manager.newService(serviceClass);
    
    if (service != null) {
      String address = service.value();
      
      if (address.startsWith("public://")
          || address.startsWith("session://")) {
        builder.setPublic(true);
      }
    }

    builder.ref();
  }
  
  public void close()
  {
    ServiceServer server = _server;
    
    if (server != null) {
      server.close();
    }
    
    ServiceManagerAmp manager = _manager;
    
    if (manager != null) {
      manager.close();
    }
  }
  
  public void closeImmediate()
  {
    ServiceServer server = _server;
    
    if (server != null) {
      server.closeImmediate();
    }
    
    ServiceManagerAmp manager = _manager;
    
    if (manager != null) {
      manager.shutdown(ShutdownModeAmp.IMMEDIATE);
    }
  }
  
  private class ScanListenerEmbed implements ScanListenerByteCode
  {
    @Override
    public ScanClass scanClass(PathImpl root, String packageRoot, 
                                String name, int modifiers)
    {
      if (! Modifier.isPublic(modifiers)) {
        return null;
      }
      
      if (Modifier.isAbstract(modifiers)) {
        return null;
      }
      
      return new ScanClassEmbed(name);
    }
    
    /**
     * Returns true if the string matches an annotation class.
     */
    @Override
    public boolean isScanMatchAnnotation(CharBuffer name)
    {
      return false;
    }

    @Override
    public void classMatchEvent(ClassLoader loader, PathImpl root,
                                String className)
    {
    }
  }
  
  private class ScanClassEmbed implements ScanClass {
    private String _name;
    private boolean _isService;
    
    ScanClassEmbed(String name)
    {
      _name = name;
    }
    
    
    /**
     * Adds a class annotation
     */
    @Override
    public void addClassAnnotation(char [] buffer, int offset, int length)
    {
      if (CharSegment.isMatch(buffer, offset, length, SERVICE)) {
        _isService = true;
      }
    }

    @Override
    public boolean finishScan()
    {
      if (! _isService) {
        return false; 
      }
      
      addService(_name);
      
      return true;
    }
  }
}

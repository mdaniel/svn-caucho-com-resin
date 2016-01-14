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

import java.util.Objects;

import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.deploy.DeployInstanceBuilder;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.L10N;

/**
 * Builder for the webapp to encapsulate the configuration process.
 */
public class PodLoaderBuilder implements DeployInstanceBuilder<PodLoader>
{
  private static final L10N L = new L10N(PodLoaderBuilder.class);
  
  private final PodLoaderController _controller;
  private EnvironmentClassLoader _classLoader;
  
  private Throwable _configException;

  private PodLoader _podLoader;

  private String _podName;

  private PodContainer _podContainer;

  private DynamicClassLoader _libraryLoader;
  
  /**
   * Builder Creates the webApp with its environment loader.
   */
  public PodLoaderBuilder(PodLoaderController controller)
  {
    Objects.requireNonNull(controller);
    
    //_podContainer = controller.getContainer();
    
    _controller = controller;
    
    //_podName = controller.getPodName();

    //_libraryLoader = new DynamicClassLoader(controller.getParentClassLoader());
    
    /*
    for (Path path : controller.getLibraryPaths()) {
      
    }
    */
    
    _classLoader
      = EnvironmentClassLoader.create(_libraryLoader,
                                      "podapp:" + getId());
    
    /*
    LibController libController = _podContainer.getLibraryController("common");

    if (libController != null) {
      LibApp libApp = libController.request();
    
      if (libApp != null) {
        _classLoader.addImportLoader(libApp.getClassLoader());
      }
    }
    */
    
    getPodLoader().initConstructor();
  }

  public PodLoaderController getController()
  {
    return _controller;
  }

  @Override
  public PodLoader getInstance()
  {
    return getPodLoader();
  }
  
  String getId()
  {
    //return _controller.getId();
    return null;
  }
  
  String getPodName()
  {
    return _podName;
  }

  public int getPodNode()
  {
    //return _controller.getPodNodeIndex();
    return 0;
  }
  
  @Override
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  @Override
  public void setConfigException(Throwable exn)
  {
    if (exn != null) {
      //getPodLoader().setConfigException(exn);
    }
  }
  
  @Override
  public Throwable getConfigException()
  {
    return _configException;
  }
  
  @Override
  public void preConfigInit()
  {
    /*
    Path libs = _controller.getRootDirectory().lookup("libs");
    new TreeLoader(_libraryLoader, libs);
    
    for (Depend depend : _controller.getDependList()) {
      _classLoader.addDependency(depend);
    }

    try {
      getPodApp().preConfigInit();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    */
    
    /*
    for (Path path : _controller.getApplicationPaths()) {
      _classLoader.addDependency(new Depend(path));
    }
    
    for (Path path : _controller.getLibraryPaths()) {
      _libraryLoader.addDependency(new Depend(path));
    }
    */
  }
  
  public void addProgram(ConfigProgram program)
  {
    // program.configure(getPodApp());
  }
  
  @Override
  public PodLoader build()
  {
    PodLoader loader = getPodLoader();

    return loader;
  }
  
  protected PodLoader getPodLoader()
  {
    if (_podLoader == null) {
      _podLoader = createPodLoader();
    }
    
    return _podLoader;
  }
  
  protected PodLoader createPodLoader()
  {
    //return new PodLoader(this);
    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}

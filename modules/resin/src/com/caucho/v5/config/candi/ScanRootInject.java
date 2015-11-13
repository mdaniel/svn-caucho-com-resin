/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.candi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.xml.ConfigXml;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

/**
 * Scanned data for the root context
 */
public class ScanRootInject implements BeanArchive
{
  private static final Logger log
    = Logger.getLogger(ScanRootInject.class.getName());
  
  private static final L10N L = new L10N(ScanRootInject.class);
  
  private static final String SCHEMA = "com/caucho/v5/config/cfg/resin-beans.rnc";
  
  private static final ThreadLocal<ScanRootInject> _localRoot
    = new ThreadLocal<>();
    
  private final Path _root;
  private ArrayList<String> _classList = new ArrayList<>();
  private boolean _isScanComplete;
  private BeansConfig _beansConfig;
  private List<String> _packageRoots;

  private CandiManager _injectManager;
  private List<ScanClassInject> _packageInfoList = new ArrayList<>();

  private BeanManagerBase _beanManager;

  public ScanRootInject(Path root,
                        String packageRoot,
                        CandiManager injectManager,
                        BeanManagerBase beanManager)
  {
    Objects.requireNonNull(root);
    Objects.requireNonNull(injectManager);
    Objects.requireNonNull(beanManager);
    
    _root = root;
    _injectManager = injectManager;
    _beanManager = beanManager;

    init(packageRoot);
  }

  private void init(String packageRoot)
  {
    Path beansXml = null;
    
    Path root = _root;

    if (packageRoot != null) {
      addPackageRoot(packageRoot);
      
      root = _root.lookup(packageRoot.replace('.', '/'));
      beansXml = root.lookup("beans.xml");
      
    }

    if (beansXml == null || ! beansXml.exists()) {
      beansXml = _root.lookup("META-INF/beans.xml");
    }

    if (beansXml.exists()) {
      _beansConfig = initFromBeansConfig(root, beansXml);
    }

    if (_beansConfig == null) {
      _beansConfig = initFromWebInf();
    }
  }
  
  protected CandiManager getInjectManager()
  {
    return _injectManager;
  }

  public BeanManagerBase getBeanManager()
  {
    return _beanManager;
  }

  void update(String packageRoot)
  {
    addPackageRoot(packageRoot);
    _isScanComplete = false;

    Path beansXml = _root.lookup(packageRoot.replace('.', '/') + "/beans.xml");

    if (beansXml.exists()) {
      _beansConfig = initFromBeansConfig(beansXml.lookup(".."), beansXml);
      return;
    }
  }

  private void addPackageRoot(String packageRoot)
  {
    if (_packageRoots == null)
      _packageRoots = new ArrayList<>();

    _packageRoots.add(packageRoot);
  }

  private BeansConfig initFromWebInf()
  {
    Path beansXml = _root.lookup("../beans.xml");

    if (beansXml.exists()) {
      return initFromBeansConfig(_root, beansXml);
    }

    beansXml = _root.lookup("META-INF/beans.xml");
    
    if (beansXml.exists()) {
      return initFromBeansConfig(_root, beansXml);
    }

    return null;
  }
  
  public static ScanRootInject getCurrent()
  {
    return _localRoot.get();
  }
  
  protected BeansConfig initFromBeansConfig(Path rootPath, Path path)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(L.l("Initializing {0} with '{1}'", this, path));

    // Path rootPath = path.lookup("../..");
    
    BeansConfig beansConfig
      = new BeansConfig(getInjectManager(), rootPath, path);

    path.setUserPath(path.getURL());

    if (path.getLength() > 0) {
      ConfigXml configXml = new ConfigXml();
      
      ScanRootInject oldRoot = _localRoot.get();
      try {
        _localRoot.set(this);
        
        configXml.configure(beansConfig, path, SCHEMA);
      } finally {
        _localRoot.set(oldRoot);
      }
    }
    
    return beansConfig;
  }

  @Override
  public BeansConfig getBeansConfig()
  {
    return _beansConfig;
  }

  @Override
  public Path getRoot()
  {
    return _root;
  }

  @Override
  public void addClassName(String className)
  {
    _classList.add(className);
  }

  @Override
  public ArrayList<String> getClassNameList()
  {
    return _classList;
  }

  @Override
  public boolean isScanComplete()
  {
    return _isScanComplete;
  }

  @Override
  public void setScanComplete(boolean isScanComplete)
  {
    _isScanComplete = isScanComplete;
  }

  @Override
  public boolean isClassExcluded(String className)
  {
    if (_beansConfig != null && _beansConfig.isExcluded(className)) {
      return true;
    }

    if (_packageRoots == null) {
      return false;
    }
    
    for (String packageRoot : _packageRoots) {
      if (className.startsWith(packageRoot + ".")) {
        return false;
      }
    }
      
    return true;
  }

  public boolean isPackageVetoed(ScanClassInject candidate)
  {
    for (ScanClassInject packageClass : _packageInfoList) {
      if (! packageClass.isVetoed())
        continue;

      if (packageClass.getPackage().equals(candidate.getPackage()))
        return true;
    }

    return false;
  }

  public void addPackageInfo(ScanClassInject scanClass)
  {
    _packageInfoList.add(scanClass);
  }

  @Override
  public boolean isImplicit()
  {
    return _beansConfig == null;
  }

  public List<Class<?>> getAlternatives()
  {
    return _beansConfig.getAlternativesList();
  }

  public List<Class<?>> getInterceptors()
  {
    return _beansConfig.getInterceptors();
  }

  public List<Class<?>> getDecorators()
  {
    return _beansConfig.getDecorators();
  }

  @Override
  public DiscoveryMode getDiscoveryMode()
  {
    DiscoveryMode mode = DiscoveryMode.ANNOTATED;

    if (_beansConfig != null) {
      mode = _beansConfig.getDiscoveryMode();
    }

    return mode;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _root + "]";
  }
}

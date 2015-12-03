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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.ejb.embeddable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;

import com.caucho.boot.ResinBootAgent;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.manager.EjbEnvironmentListener;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.inject.Module;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.naming.AbstractModel;
import com.caucho.naming.ContextImpl;
import com.caucho.naming.InitialContextFactoryImpl;
import com.caucho.server.e_app.EnterpriseApplication;
import com.caucho.server.webbeans.ResinCdiProducer;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Interface for the EJBClient.
 */
@Module
public class EJBContainerImpl extends EJBContainer {
  private static final Logger log 
    = Logger.getLogger(EJBContainerImpl.class.getName());
  
  public static final String CONFIG_FILE = "caucho.ejb.embed.config.file";

  private Context _context;
  private ClassLoader _parentClassLoader;
  private EnvironmentClassLoader _classLoader;
  private InjectManager _injectManager;
  private EnterpriseApplication _application;
  private ArrayList<Path> _moduleRoots;
  private Map<?,?> _properties;

  public EJBContainerImpl()
    throws EJBException
  {
    this(null, new HashMap<String,String>());
  }

  public EJBContainerImpl(String name,
                          Map<?,?> properties)
    throws EJBException
  {
    preInit(name);
    
    _properties = properties;
  }

  void addModule(Path path)
  {
    if (_moduleRoots == null) {
      _moduleRoots = new ArrayList<Path>();
    }

    _moduleRoots.add(path);
  }

  void preInit(String name)
  {
    if (_application != null)
      return;
    
    Thread thread = Thread.currentThread();
    
    _parentClassLoader = thread.getContextClassLoader(); 
    _application = EnterpriseApplication.create(name);
    
    _classLoader = _application.getClassLoader();
    _injectManager = InjectManager.create(_classLoader);
    
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      Environment.init();

      Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
      Environment.addChildLoaderListener(new EjbEnvironmentListener());

      _injectManager.addManagedBeanDiscover(_injectManager.createManagedBean(ResinCdiProducer.class));

      Class<?> resinValidatorClass = ResinCdiProducer.createResinValidatorProducer();
      
      if (_injectManager != null && resinValidatorClass != null) {
        _injectManager.addManagedBeanDiscover(_injectManager.createManagedBean(resinValidatorClass));
      }
   
      // XXX initialcontextfactory broken when set by non-resin container
      AbstractModel model = InitialContextFactoryImpl.createRoot();
      _context = new ContextImpl(model, null);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);
      
      configure();
      if (_moduleRoots != null) {
        for (Path path : _moduleRoots) {
          _classLoader.addURL(new URL(path.getURL()));
        }

        EjbManager manager = EjbManager.getCurrent();

        manager.setGlobalClassLoader(_parentClassLoader);
        manager.setScannableRoots(_moduleRoots);
      }
      
      _classLoader.addScanRoot();
      _application.start();
    }
    catch (MalformedURLException e) {
      log.log(Level.FINE, e.toString(), e);
    } 
    finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void configure()
  {
    String configFile = (String) _properties.get(CONFIG_FILE);
    
    if (configFile == null)
      configFile = System.getProperty(CONFIG_FILE);
    
    if (configFile == null)
      return;
    
    Path path = Vfs.lookup(configFile);
    
    Config config = new Config();
    EnvironmentConfig configItem = new EnvironmentConfig();

    try {
      config.configure(configItem, path);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  @Override
  public Context getContext()
  {
    return _context;
  }

  @Override
  public void close()
  {
    _classLoader.destroy();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _application.getName() + "]";
  }
  
  class EnvironmentConfig implements EnvironmentBean {
    @Override
    public ClassLoader getClassLoader()
    {
      return _classLoader;
    }
  }
}

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
 */

package com.caucho.v5.config.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.inject.BindingInject;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * The ConfigContext contains the state of the current configuration.
 */
@ModulePrivate
public class ContextConfig
{
  private final static Logger log
    = Logger.getLogger(ContextConfig.class.getName());

  public final static NameCfg TEXT = new NameCfg("#text");

  private static ThreadLocal<ContextConfig> _currentContext
    = new ThreadLocal<>();

  private ArrayList<Dependency> _dependList = new ArrayList<>();
  
  private final ConfigContext _config;

  //private InjectionPoint _ij;
  
  private InjectContext _beanStack;

  private String _baseUri;

  public ContextConfig(ContextConfig parent)
  {
    this(parent.getConfig());
  }

  public ContextConfig(ConfigContext config)
  {
    Objects.requireNonNull(config);

    _config = config;
  }

  /*
  public static ContextConfig create()
  {
    ContextConfig env = _currentBuilder.get();

    if (env != null)
      return env;
    else
      return new ContextConfig();
  }
  */

  public static ContextConfig getCurrent()
  {
    return _currentContext.get();
  }

  public static void setCurrent(ContextConfig builder)
  {
    _currentContext.set(builder);
  }

  /*
  public InjectionPoint getInjectionPoint()
  {
    return _ij;
  }

  public void setInjectionPoint(InjectionPoint ij)
  {
    _ij = ij;
  }
  */

  /**
   * Returns the file var
   */
  public String getBaseUriDecoded()
  {
    return VfsOld.decode(_baseUri);
  }

  /**
   * Returns the file var
   */
  public String getBaseUri()
  {
    return _baseUri;
  }
  
  public void setBaseUri(String uri)
  {
    _baseUri = uri;
  }

  /**
   * Returns the component value for the dependent scope
   *
   * @param aThis
   * @return
   */
  public Object get(BindingInject<?> bean)
  {
    return InjectContext.find(_beanStack, bean);
  }
  
  public Object findByName(String name)
  {
    return InjectContext.findByName(_beanStack, name);
  }
  
  public InjectContext setCreationalContext(InjectContext cxt)
  {
    InjectContext oldCxt = _beanStack;
    
    _beanStack = cxt;
    
    return oldCxt;
  }
  
  public InjectContext getCreationalContext()
  {
    return _beanStack;
  }

  public ConfigContext getConfig()
  {
    return _config;
  }

  /**
   * Returns true if EL expressions are used.
   */
  public boolean isEL()
  {
    // server/26b6
    return _config == null || _config.isEL();
  }

  public boolean isIgnoreEnvironment()
  {
    return _config != null && _config.isIgnoreEnvironment();
  }

  public ArrayList<Dependency> getDependencyList()
  {
    return _dependList;
  }
  
  protected void setDependencyList(ArrayList<Dependency> dependList)
  {
    _dependList = dependList;
  }

  /**
   * Returns the variable resolver.
   */
  /*
  public static ConfigELContext getELContext()
  {
    return ConfigELContext.EL_CONTEXT;
  }
  */

  public static String getSourceLines(String systemId, int errorLine)
  {
    if (systemId == null)
      return "";

    ReadStream is = null;
    try {
      is = VfsOld.lookup().lookup(systemId).openRead();
      int line = 0;
      StringBuilder sb = new StringBuilder("\n\n");
      String text;
      while ((text = is.readLine()) != null) {
        line++;

        if (errorLine - 2 <= line && line <= errorLine + 2) {
          sb.append(line);
          sb.append(": ");
          sb.append(text);
          sb.append("\n");
        }
      }

      return sb.toString();
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);

      return "";
    } finally {
      if (is != null)
        is.close();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

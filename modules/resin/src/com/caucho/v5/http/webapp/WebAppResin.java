/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.el.CandiElResolver;
import com.caucho.v5.jsp.cfg.JspConfig;
import com.caucho.v5.jsp.cfg.JspPropertyGroup;
import com.caucho.v5.jsp.cfg.JspTaglib;
import com.caucho.v5.jsp.el.JspApplicationContextImpl;

/**
 * Resin's webApp implementation.
 */
public class WebAppResin extends WebApp
{
  private static final Logger log
    = Logger.getLogger(WebAppResin.class.getName());
  
  private JspApplicationContextImpl _jspApplicationContext;
  
  //private JspPropertyGroup _jsp = new JspPropertyGroup();
  // private RestContainer _restContainer;
  private WebAppBuilderResin _builder;

  private CandiManager _cdiManager;

  private boolean _isContainerInit;

  /**
   * Creates the webApp with its environment loader.
   */
  WebAppResin(WebAppBuilderResin builder)
  {
    super(builder);
    
    _builder = builder;
  }
  
  public static WebAppResin getCurrent()
  {
    return (WebAppResin) WebApp.getCurrent();
  }
  
  @Override
  public WebAppDispatchResin getDispatcher()
  {
    return (WebAppDispatchResin) super.getDispatcher();
  }

  @Override
  protected WebAppDispatch createWebAppDispatch(WebAppBuilder builder)
  {
    return new WebAppDispatchResin((WebAppBuilderResin) builder);
  }

  /**
   * Returns the JSP configuration.
   */
  public JspPropertyGroup getJsp()
  {
    return _builder.getJsp();
  }
  
  @Override
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    if (_isContextConfigUnsuppored) {
      throw new UnsupportedOperationException("Can't call getJspConfigDescriptor() from this context");
    }

    return getBuilder().getJspConfigDescriptor();
  }

  public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
  {
    return getBuilder().getJspPropertyGroups();
  }

  public boolean isFacesServletConfigured()
  {
    return getDispatcher().isFacesServletConfigured();
  }

  public JspConfig getJspConfig()
  {
    return getBuilder().getJspConfig();
  }

  /**
   * Returns true for JSP 1.x
   */
  public boolean hasPre23Config()
  {
    return getBuilder().hasPre23Config();
  }
  
  @Override
  public WebAppBuilderResin getBuilder()
  {
    return _builder;
  }

  @Override
  protected void startInContext()
  {
    super.startInContext();
    
    _jspApplicationContext.addELResolver(new CandiElResolver());

    // initRest();
  }

  @Override
  protected void initConstructor()
  {
    super.initConstructor();
    
    _jspApplicationContext = new JspApplicationContextImpl(this);
    _jspApplicationContext.addELResolver(getBeanManager().getELResolver());

    _cdiManager = CandiManager.create(getClassLoader());
  }
  
  @Override
  public void init() throws Exception
  {
    super.init();

    try {
      initCdiJsfContext();
    } catch (NoClassDefFoundError e) {
      log.fine(e.toString());
    
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }
  
  private CandiManager getBeanManager()
  {
    return CandiManager.getCurrent(getClassLoader());
  }

  public JspApplicationContextImpl getJspApplicationContext()
  {
    return _jspApplicationContext;
  }

  public ArrayList<JspTaglib> getTaglibList()
  {
    return _builder.getTaglibList();
  }

  /*
  public RestContainer getRestContainer()
  {
    return _restContainer;
  }
  */
  
  private void initCdiJsfContext()
  {
    try {
      String jsf = "javax.faces.application.ViewHandlerWrapper";
      
      Class<?> jsfCl = Class.forName(jsf, false, getClassLoader());
      
      String handler = "com.caucho.v5.server.cdi.ConversationJsfViewHandler";
      
      Class<?> cl = Class.forName(handler, false, getClassLoader());
      
      if (cl != null && cl.getMethods() != null) {
        getClassLoader().putResourceAlias("META-INF/faces-config.xml",
                                                     "META-INF/faces-config.xml.in");
        
        getClassLoader().putResourceAlias("META-INF/services/com.sun.faces.spi.injectionprovider",
                                                     "META-INF/services/com.sun.faces.spi.injectionprovider.in");
      }
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }

  @Override
  protected boolean isContainerInit()
  {
    return _isContainerInit;
  }

  @Override
  protected void callInitializers() throws Exception
  {
    try {
      assert _isContainerInit == false;

      _isContainerInit = true;
      getBuilder().getScanner().callInitializers();
    } finally {
      _isContainerInit = false;
    }
  }

  @Override
  protected void initWebFragments()
  {
    if (! getBuilder().isMetadataComplete()) {
      getBuilder().initWebFragments();
    }
  }
}

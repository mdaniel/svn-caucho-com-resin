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

package com.caucho.v5.jsp.el;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.jsp.JspApplicationContext;

import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.jsp.PageManager;
import com.caucho.v5.jsp.TaglibManager;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.L10N;
//import com.caucho.v5.el.StreamELResolver;

public class JspApplicationContextImpl implements JspApplicationContext
{
  private static final L10N L = new L10N(JspApplicationContextImpl.class);
  
  private static final EnvironmentLocal<JspApplicationContextImpl> _contextLocal
    = new EnvironmentLocal<JspApplicationContextImpl>();
  
  private final WebAppResinBase _webApp;
  
  private final ExpressionFactory _expressionFactory;

  private TaglibManager _taglibManager;
  private PageManager _pageManager;

  private ConcurrentArrayList<ELResolver> _resolverArray
    = new ConcurrentArrayList<>(ELResolver.class);
    
  private ConcurrentArrayList<ELContextListener> _listenerArray
    = new ConcurrentArrayList<>(ELContextListener.class);

  private final AtomicBoolean _hasRequest = new AtomicBoolean();

  public JspApplicationContextImpl(WebAppResinBase webApp)
  {
    _webApp = webApp;

    //InjectManager injectManager = InjectManager.current(_webApp.getClassLoader());
    
    ExpressionFactory factory = new JspExpressionFactoryImpl(this);
    
    //_expressionFactory = injectManager.wrapExpressionFactory(factory);
    _expressionFactory = factory;
    
    _contextLocal.set(this);
  }
  
  public static JspApplicationContextImpl getCurrent()
  {
    return _contextLocal.get();
  }

  //
  // properties
  //
  
  WebAppResinBase getWebApp()
  {
    return _webApp;
  }

  public TaglibManager getTaglibManager()
  {
    return _taglibManager;
  }

  public void setTaglibManager(TaglibManager taglibManager)
  {
    _taglibManager = taglibManager;
  }

  public PageManager getPageManager()
  {
    if (_pageManager == null)
      throw new NullPointerException();
    
    return _pageManager;
  }

  public void setPageManager(PageManager pageManager)
  {
    _pageManager = pageManager;
  }

  //
  // JspApplicationContext API methods
  //
  
  /**
   * Adds an ELContextListener.
   */
  @Override
  public void addELContextListener(ELContextListener listener)
  {
    if (_hasRequest.get()) {
      throw new IllegalStateException(L.l("Cannot add ELContextListener after requests have started."));
    }
    
    _listenerArray.add(listener);
  }

  public ELContextListener []getELListenerArray()
  {
    startRequest();

    return _listenerArray.toArray();
  }
  
  /**
   * Adds an ELResolver
   */
  @Override
  public void addELResolver(ELResolver resolver)
  {
    if (_hasRequest.get()) {
      throw new IllegalStateException(L.l("Can't add ELResolver after starting request."));
    }
    
    _resolverArray.add(resolver);
  }

  public ELResolver []getELResolverArray()
  {
    startRequest();

    return _resolverArray.toArray();
  }
  
  private void startRequest()
  {
    _hasRequest.compareAndSet(false, true);
  }
  
  /**
   * Gets the expression factory
   */
  @Override
  public ExpressionFactory getExpressionFactory()
  {
    return _expressionFactory;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _webApp + "]";
  }
}

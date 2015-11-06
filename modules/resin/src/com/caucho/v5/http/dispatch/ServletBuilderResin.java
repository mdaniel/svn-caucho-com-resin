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

package com.caucho.v5.http.dispatch;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.caucho.v5.config.inject.BeanBuilder;
import com.caucho.v5.config.inject.CandiManager;
import com.caucho.v5.http.webapp.WebAppResin;
import com.caucho.v5.jsp.Page;
import com.caucho.v5.jsp.QServlet;
import com.caucho.v5.naming.JndiUtil;
import com.caucho.v5.util.L10N;

/**
 * Configuration for a servlet.
 */
public class ServletBuilderResin extends ServletBuilder
{
  private static final L10N L = new L10N(ServletBuilderResin.class);
  
  // private ProtocolServletFactory _protocolFactory;
  private ServletProtocolConfig _protocolConfig;

  private String _jndiName;
  private String _var;

  /**
   * Creates a new servlet configuration object.
   */
  public ServletBuilderResin()
  {
  }

  public ServletBuilderResin(FragmentMode fragmentMode)
  {
    super(fragmentMode);
  }

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the web service protocol.
   */
  public void setProtocol(ServletProtocolConfig protocol)
  {
    _protocolConfig = protocol;
  }

  /**
   * Sets the web service protocol.
   */
  /*
  public void setProtocolFactory(ProtocolServletFactory factory)
  {
    _protocolFactory = factory;
  }
  */

  @Override
  protected void initServletImpl(Object servlet)
    throws ServletException
  {
    if (servlet instanceof Page) {
      
    }
    else {
      super.initServletImpl(servlet);
    }
  }

  @Override
  protected Object createServletInstance()
    throws Exception
  {
    if (getJspFile() != null) {
      Object servlet = createJspServlet(getServletName(), getJspFile());

      if (servlet == null)
        throw new ServletException(L.l("'{0}' is a missing JSP file.",
                                       getJspFile()));
      
      return servlet;
    }

    return super.createServletInstance();
  }

  @Override
  protected FilterChain createServletChainImpl()
    throws ServletException
  {
    String jspFile = getJspFile();
    FilterChain servletChain = null;

    if (jspFile != null) {
      QServlet jsp = (QServlet) getServletManager().createServlet("resin-jsp");

      servletChain = new PageFilterChain(getServletContext(), jsp, jspFile, this);

      return servletChain;
    }
    
    return super.createServletChainImpl();
  }
  
  @Override
  protected FilterChain createServletChain(Class<?> servletClass)
    throws ServletException
  {
    if (QServlet.class.isAssignableFrom(servletClass)) {
      return  new PageFilterChain(getServletContext(), 
                                  (QServlet) createServlet(false));
    }
    /*
    else if (_protocolConfig != null || _protocolFactory != null) {
      return new WebServiceFilterChain(this);
    }
    */
    else {
      return super.createServletChain(servletClass);
    }
  }

  @Override
  protected boolean isProtocolServlet()
  {
    // return _protocolConfig != null || _protocolFactory != null;
    return false;
  }

  /**
   * Initialize the servlet config.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    /*
    if (javax.ws.rs.core.Application.class.getName().equals(getServletName())
        && getServletClassName() == null
        && getServletClass() == null) {
      setServletClass(RestServlet.class.getName());
    }
    */
    
    super.init();

    // XXX: should only be for web services
    if (_jndiName != null) {
      validateClass(true);

      Object servlet = createServlet(false);

      try {
        JndiUtil.bindDeepShort(_jndiName, servlet);
      } catch (NamingException e) {
        throw new ServletException(e);
      }
    }

    if (_var != null) {
      CandiManager cdiManager = CandiManager.getCurrent();
      
      validateClass(true);

      Object servlet = createServlet(false);

      BeanBuilder factory = cdiManager.createBeanBuilder(servlet.getClass());
      factory.name(_var);

      cdiManager.addBeanDiscover(factory.singleton(servlet));
    }
  }


  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  private Servlet createJspServlet(String servletName, String jspFile)
    throws ServletException
  {
    try {
      ServletBuilder jspConfig = getServletManager().getServlet("resin-jsp");

      QServlet jsp = (QServlet) jspConfig.createServlet(false);

      // server/105o
      Page page = jsp.getPage(servletName, jspFile, this);

      return page;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Instantiates a web service.
   *
   * @return the initialized servlet.
   */
  /*
  ProtocolServlet createWebServiceSkeleton()
    throws ServletException
  {
    try {
      Object service = createServlet(false);

      ProtocolServlet skeleton
        = (ProtocolServlet) _protocolClass.newInstance();

      skeleton.setService(service);

      if (_protocolInit != null) {
        _protocolInit.configure(skeleton);
      }

      skeleton.init(this);

      return skeleton;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  */
  
  
  @Override
  protected void loadTld()
  {
    ((WebAppResin) getWebApp()).getJsp().setLoadTldOnInit(true);
  }


  Servlet createProtocolServlet()
    throws ServletException
  {
    try {
      Object service = createServletImpl();

      /*
      if (_protocolFactory == null)
        _protocolFactory = _protocolConfig.createFactory();

      if (_protocolFactory == null)
        throw new IllegalStateException(L.l("unknown protocol factory for '{0}'",
                                            this));

      Servlet servlet
        = _protocolFactory.createServlet(getServletClass(), service);
      
      servlet.init(this);

      return servlet;
      */
      
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
}

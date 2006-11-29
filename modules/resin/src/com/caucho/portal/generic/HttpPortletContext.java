/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.servlet.ServletContext;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * HttpPortletContext provides the connection to a web application
 * (ServletContext).
 */ 
public class HttpPortletContext
  implements PortletContext
{
  static protected final Logger log = 
    Logger.getLogger(HttpPortletContext.class.getName());

  private ServletContext _servletContext;

  public HttpPortletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;
  }

  /**
   * {@inheritDoc}
   */
  public String getServerInfo()
  {
    return _servletContext.getServerInfo();
  }

  /**
   * {@inheritDoc}
   */
  public int getMinorVersion()
  {
    return _servletContext.getMinorVersion();
  }

  /**
   * {@inheritDoc}
   */
  public int getMajorVersion()
  {
    return _servletContext.getMajorVersion();
  }

  /**
   * {@inheritDoc}
   */
  public PortletRequestDispatcher getRequestDispatcher(String path)
  {
    HttpPortletRequestDispatcher portletDispatcher
      = new HttpPortletRequestDispatcher();

    if (portletDispatcher.startWithPath(_servletContext, path))
      return portletDispatcher;
    else {
      portletDispatcher.finish();
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public PortletRequestDispatcher getNamedDispatcher(String name)
  {
    HttpPortletRequestDispatcher portletDispatcher
      = new HttpPortletRequestDispatcher();

    if (portletDispatcher.startWithName(_servletContext, name)) {
      return portletDispatcher;
    }
    else {
      portletDispatcher.finish();
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public InputStream getResourceAsStream(String path)
  {
    return _servletContext.getResourceAsStream(path);
  }

  /**
   * {@inheritDoc}
   */
  public String getMimeType(String file)
  {
    return _servletContext.getMimeType(file);
  }

  /**
   * {@inheritDoc}
   */
  public String getRealPath(String path)
  {
    return _servletContext.getRealPath(path);
  }

  /**
   * {@inheritDoc}
   */
  public Set getResourcePaths(String path)
  {
    return _servletContext.getResourcePaths(path);
  }

  /**
   * {@inheritDoc}
   */
  public URL getResource(String path) throws java.net.MalformedURLException
  {
    return _servletContext.getResource(path);
  }

  /**
   * {@inheritDoc}
   */
  public String getInitParameter(String name)
  {
    return _servletContext.getInitParameter(name);
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getInitParameterNames()
  {
    return _servletContext.getInitParameterNames();
  }

  /**
   * {@inheritDoc}
   */
  public Object getAttribute(String name)
  {
    return _servletContext.getAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getAttributeNames()
  {
    return _servletContext.getAttributeNames();
  }

  /**
   * {@inheritDoc}
   */
  public void removeAttribute(String name)
  {
    _servletContext.removeAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  public void setAttribute(String name, Object object)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("setAttribute(" + name  + ")");
    }
    _servletContext.setAttribute(name,object);
  }

  /**
   * {@inheritDoc}
   */
  public void log(String msg)
  {
    log(msg, null);
  }

  /**
   * {@inheritDoc}
   */
  public void log(String message, Throwable e)
  {
    if (e != null)
      log.log(Level.WARNING, message, e);
    else
      log.info(message);
  }

  /**
   * {@inheritDoc}
   */
  public String getPortletContextName()
  {
    return _servletContext.getServletContextName();
  }

}


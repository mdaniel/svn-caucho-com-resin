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

import javax.portlet.PortletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

/**
 * This servlet supports the following configuration items.
 * Items marked with a * can be set as init-param.
 *
 * <dl>
 * <dd>portal 
 * <dt>an instance of {@link Portal}, default is an instance of 
 *     {@link GenericPortal}. 
 * <dt>portal-class*
 * <dd>a class name, an alternative to <code>portal</code>
 * <dd>layout
 * <dt>an instance of {@link GenericLayoutWindow}, required
 * <dt>layout-class*
 * <dd>a class name, an alternative to <code>layout</code>
 *
 * </dl> 
 * </pre> 
 */
public class GenericPortalServlet
  extends HttpServlet
{
  static protected final Logger log = 
    Logger.getLogger(GenericPortalServlet.class.getName());

  private Portal _portal;
  private HttpPortletContext _portletContext;

  private GenericLayoutWindow _layout;

  /**
   * Default is an instance of {@link GenericPortal}.
   */
  public void setPortal(Portal portal)
  {
    if (_portal != null)
      throw new IllegalArgumentException("`portal' already set");

    _portal = portal;
  }

  /**
   * An alternative to {@link #setPortal(Portal)}, specify the class
   * name of an object to instantiate
   */
  public void setPortalClass(String className)
  {
    setPortal( (Portal) newInstance(Portal.class, className) );
  }

  /**
   * The layout, required.  This method can be called in derived classes,
   * or through the use of dependency injection on containers that support it,
   * or indirectly using the init param `layout-class'.
   */
  public void setLayout(GenericLayoutWindow layout)
  {
    if (_layout != null)
      throw new IllegalArgumentException("`layout' is already set");

    _layout = layout;
  }


  /**
   * An alternative to {@link #setLayout(GenericLayoutWindow)}, specify the
   * class name of an object to instantiate
   */
  public void setLayoutClass(String className)
  {
    setLayout( (GenericLayoutWindow) newInstance( GenericLayoutWindow.class, 
                                                  className ) );
  }

  public void init(ServletConfig servletConfig)
    throws ServletException
  {
    super.init(servletConfig);

    String p;

    p = super.getInitParameter("portal-class");
    if (p != null)
      setPortalClass( p );

    if (_portal == null)
      _portal = new GenericPortal();

    p = super.getInitParameter("layout-class");
    if (p != null)
      setLayoutClass( p );

    if (_layout == null)
      throw new ServletException("`layout' is required");

    _portletContext = new HttpPortletContext(getServletContext());

    try {
      _layout.init(_portletContext);
    }
    catch (PortletException ex) {
      throw new ServletException(ex);
    }
  }

  protected Object newInstance(Class targetClass, String className)
    throws IllegalArgumentException
  {
    Class cl = null;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      cl = Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
    }

    if (cl == null)
      throw new IllegalArgumentException(
          "`" + className + "' is not a known class");

    if (!targetClass.isAssignableFrom(cl))
      throw new IllegalArgumentException(
          "'" + className + "' must implement " + targetClass.getName());

    if (Modifier.isAbstract(cl.getModifiers()))
      throw new IllegalArgumentException(
          "'" + className + "' must not be abstract.");

    if (!Modifier.isPublic(cl.getModifiers()))
      throw new IllegalArgumentException(
          "'" + className + "' must be public.");

    Constructor []constructors = cl.getDeclaredConstructors();

    Constructor zeroArg = null;
    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
        zeroArg = constructors[i];
        break;
      }
    }

    if (zeroArg == null || !Modifier.isPublic(zeroArg.getModifiers()))
      throw new IllegalArgumentException(
          "'" + className + "' must have a public zero arg constructor");

    Object obj = null;

    try {
      obj =  cl.newInstance();
    }
    catch (Exception ex) {
      throw new IllegalArgumentException(
          "error instantiating `" + className + "': " + ex.toString(), ex);
    }

    return obj;
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    doRequest(req, res);
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    doRequest(req, res);
  }

  protected void doRequest(HttpServletRequest httpRequest, 
                           HttpServletResponse httpResponse)
    throws ServletException, IOException
  {     
    HttpPortletConnection connection = new HttpPortletConnection();
    connection.start(_portal, _portletContext, httpRequest, httpResponse, true);

    try {
      _layout.processAction(connection);
      _layout.render(connection);
      connection.checkForFailure();
    }
    catch (PortletException ex) {
      throw new ServletException(ex);
    }
    finally {
      connection.finish();
    }
  }

  protected void doRequest(PortletConnection connection)
    throws PortletException, IOException
  {     
  }
  public void destroy()
  {
    if (_layout != null)
      _layout.destroy();
  }
}


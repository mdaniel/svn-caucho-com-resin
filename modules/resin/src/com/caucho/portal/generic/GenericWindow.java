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

import java.io.IOException;

import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

abstract public class GenericWindow
  implements Window, PortletConfig
{
  static protected final Logger log = 
    Logger.getLogger(GenericWindow.class.getName());

  private String _namespace = "";
  private String _portletName;
  private Map<String, String> _initParamMap;
  private int _expirationCache;
  private boolean _isPrivate;
  private Renderer _renderer;
  private int _bufferSize;
  private Set<Locale> _supportedLocales;
  private GenericPortletPreferences _defaultPreferences;
  private ResourceBundleFactory _resourceBundleFactory;
  private String _errorPage;

  private PortletContext _portletContext;

  public GenericWindow()
  {
  }
 
  /**
   * The namespace is used to uniquely identify this usage of the portlet,
   * the default is "" (the empty string).
   *
   * The namespace is important when using a portlet preferences store,
   * and also has an effect on the encoding of parameters.
   */
  public void setNamespace(String namespace)
  {
    _namespace = namespace;
  }

  /**
   * The default is the namespace.
   */
  public void setPortletName(String portletName)
  {
    _portletName = portletName;
  }

  /**
   * Add an init-param for the portlet. 
   */
  public void addInitParam(String name, String value)
  {
    if (_initParamMap == null)
      _initParamMap = new LinkedHashMap<String, String>();

    _initParamMap.put(name, value);
  }

  public void addInitParam(NameValuePair nameValuePair)
  {
    addInitParam(nameValuePair.getName(), nameValuePair.getValue());
  }

  /**
   * Set the default preferences.
   */
  public void setPortletPreferences(GenericPortletPreferences defaultPreferences)
  {
    _defaultPreferences = defaultPreferences;
  }

  /**
   * Enable caching of the response and set the expiration time in seconds. 0
   * (the default) means do not cache, -1 means unlimited cach time, any other
   * number is the number of seconds for which the response can be cached.
   */ 
  public void setExpirationCache(int expirationCache)
  {
    _expirationCache = expirationCache;
  }

  /**
   * If true then the response is private, indicating that it contains
   * information that should only be provided to the current client, default is
   * false.  Setting this to true has an effect on caching, if true then a
   * cached value
   * cannot be shared amongst different users and the effectiveness of caching
   * is greatly reduced.
   */
  public void setPrivate(boolean isPrivate)
  {
    _isPrivate = isPrivate;
  }

  /**
   * Add a supported locale, the default is to support all locales.
   * This is an ordered list, those added first are more preferrable.
   */
  void addSupportedLocale(String locale)
  {
    String language = "";
    String country = "";
    String variant = "";

    String[] split = locale.split("_", 3);
    int len = split.length;

    if (len == 0) {
      split = locale.split("-", 3);
      len = split.length;
    }

    if (len == 0)
      throw new IllegalArgumentException(locale);

    language = split[0];
    if (len > 0)
      country = split[1];
    if (len > 1)
      country = split[2];
    
    if (_supportedLocales == null)
      _supportedLocales = new LinkedHashSet<Locale>();

    _supportedLocales.add(new Locale(language, country, variant));
  }

  /**
   * Add supported locales with a comma separated list, the default is to
   * support all locales.  This is an ordered list, those added first are more
   * preferrable.
   */
  void addSupportedLocales(String locales)
  {
    String[] split = locales.split("\\s*,\\s*");
    for (int i = 0; i < split.length; i++)
      addSupportedLocale(split[i]);
  }

  /**
   * Set a resource bundle name, used to instantiate an instance of
   * ResourceBundleFactory.
   */
  public void setResourceBundle(String name)
  {
    ResourceBundleFactory resourceBundleFactory = 
      new ResourceBundleFactory();

    resourceBundleFactory.setName(name);

    setResourceBundleFactory(resourceBundleFactory);
  }

  public void setResourceBundleFactory(ResourceBundleFactory factory)
  {
    if (_resourceBundleFactory != null)
      throw new IllegalArgumentException("resource-bundle-factory already set");
  }

  /**
   * A Renderer wraps decorations around the portlet, see 
   * {@link AbstractRenderer}.
   */
  public void setRenderer(Renderer renderer)
  {
    _renderer = renderer;
  }

  /**
   * An alternative to {@link #setRenderer(Renderer)}, specify the class
   * name of an object to instantiate
   */
  public void setRendererClass(String className)
  {
    setRenderer( (Renderer) newInstance(Renderer.class, className) );
  }

  /**
   * 0 disables buffering, -1 allows the portal to choose a 
   * buffer size, a positive number indicaets a minimum buffer size. Default is
   * 0 unless `error-page' ({@link #setErrorPage(String)} has been used, then
   * the default is -1.
   */
  public void setBufferSize(int bufferSize)
  {
    _bufferSize = bufferSize;
  }

  /**
   * Specify a location to forward to if an exception or constraint failure
   * occurs while rendering the portlet.  The default behaviour is
   * for an exception or constraint failure to propogate to the parent
   * window, or if there is no parent then to the servlet container.
   *
   * If an exception occurs the following request attributes are set:
   *
   * <dl> 
   * <dt>com.caucho.portal.error.exception
   * <dd>java.lang.Throwable
   *
   * <dt>com.caucho.portal.error.exception_type
   * <dd>java.lang.Class
   *
   * <dt>com.caucho.portal.error.message
   * <dd>java.lang.String
   *
   * <dt>javax.portlet.renderRequest
   * <dd>javax.portlet.RenderRequest
   *
   * <dt>javax.portlet.renderResponse
   * <dd>javax.portlet.RenderResponse
   *
   * <dt>javax.portlet.portletConfig
   * <dd>javax.portlet.PortletConfig
   *
   * </dl> 
   *
   * If a constraint failure occurs the following request attributes are set:
   *
   * <dl> 
   * <dt>com.caucho.portal.error.constraint
   * <dd>com.caucho.portal.generic.Constraint
   *
   * <dt>com.caucho.portal.error.constraint_type
   * <dd>java.lang.Class
   *
   * <dt>com.caucho.portal.error.status_code
   * <dd>java.lang.Integer
   *
   * <dt>javax.portlet.renderRequest
   * <dd>javax.portlet.RenderRequest
   *
   * <dt>javax.portlet.renderResponse
   * <dd>javax.portlet.RenderResponse
   *
   * <dt>javax.portlet.portletConfig
   * <dd>javax.portlet.PortletConfig
   *
   * </dl> 
   */ 
  public void setErrorPage(String errorPage)
  {
    _errorPage = errorPage;

    if (_bufferSize == 0)
      _bufferSize = -1;
  }

  public void init(PortletContext portletContext)
    throws PortletException
  {
    if (_portletContext != null)
      throw new IllegalStateException("portlet-context already set!");

    _portletContext = portletContext;

    if (_portletName == null)
      _portletName = _namespace == null ? "anonymous" : _namespace;
  }

  /**
   * Instantiate a new instance of an object, performing checks
   * for validity.
   *
   * The object that results from instantiating an instance of
   * <code>className</code> must be an <code>instance of</code>
   * <code>targetClass</code>.
   *
   * @param targetClass the class that the instantiated Object should be
   * compatible with. 
   *
   * @param className the String name of a class to use when instantiating the
   * object.
   *
   * @return a new Object 
   */ 
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

  protected String getNamespace()
  {
    return _namespace;
  }

  /**
   * {@inheritDoc}
   */
  public PortletContext getPortletContext()
  {
    if (_portletContext == null)
      throw new IllegalStateException("missing init()?");

    return _portletContext;
  }

  /**
   * {@inheritDoc}
   */
  public PortletConfig getPortletConfig()
  {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public String getPortletName()
  {
    return _portletName;
  }

  public String getInitParameter(String name)
  {
    if (_initParamMap == null)
      return null;
    else
      return _initParamMap.get(name);
  }

  public Enumeration getInitParameterNames()
  {
    if (_initParamMap == null)
      return Collections.enumeration(Collections.EMPTY_LIST);
    else
      return Collections.enumeration(_initParamMap.keySet());
  }

  /**
   * {@inheritDoc}
   */ 
  public int getExpirationCache()
  {
    return _expirationCache;
  }

  /**
   * {@inheritDoc}
   */ 
  public boolean isPrivate()
  {
    return _isPrivate;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns true.
   */ 
  public boolean isWindowStateAllowed(PortletRequest request,
                                      WindowState windowState)
  {
    // XXX: todo: support a window-states init-param like
    // normal, minimized
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns true.
   */ 
  public boolean isPortletModeAllowed(PortletRequest request,
                                      PortletMode portletMode)
  {
    // todo: see getSupportedContentTypes()
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns null, which means that all content
   * types are supported.
   */ 
  public Set<String> getSupportedContentTypes(PortletMode portletMode)
  {
    // XXX: todo: support a content-type init-param like
    // edit(text/html), view(text/html, application/pdf)
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns null, which means that all locales are
   * supported.
   */ 
  public Set<Locale> getSupportedLocales()
  {
    return _supportedLocales;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns null.
   */ 
  public PortletPreferences getDefaultPreferences()
  {
    return _defaultPreferences;
  }

  /**
   * {@inheritDoc}
   */ 
  public ArrayList<PreferencesValidator> getPreferencesValidators()
  {
    if (_defaultPreferences != null)
      return _defaultPreferences.getPreferencesValidators();
    else
      return null;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns null.
   */ 
  public Map<String, String> getRoleRefMap()
  {
    return null;
    // todo
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns null.
   */ 
  public ArrayList<Constraint> getConstraints()
  {
    return null;
    // todo
  }

  /**
   * {@inheritDoc}
   */
  public Renderer getRenderer()
  {
    return _renderer;
  }

  /**
   * {@inheritDoc}
   *
   * This implementation returns PortletMode.VIEW. 
   */
  public PortletMode handlePortletModeFailure( PortletRequest request, 
                                               PortletMode notAllowed )
  {
    return PortletMode.VIEW;
  }


  /**
   * {@inheritDoc}
   *
   * This implementation returns WindowState.NORMAL. 
   */
  public WindowState handleWindowStateFailure( PortletRequest request, 
                                               WindowState notAllowed )
  {
    return WindowState.NORMAL;
  }

  /**
   * {@inheritDoc}
   *
   * @see #setErrorPage(String)
   */ 
  public void handleConstraintFailure( RenderRequest request, 
                                       RenderResponse response, 
                                       ConstraintFailureEvent event )
  {
    if (_errorPage == null)
      return;

    Object existingConstraint = 
      request.getAttribute("com.caucho.portal.error.constraint");

    Object existingConstraintType = 
      request.getAttribute("com.caucho.portal.error.constraint_type");

    Object existingStatusCode = 
      request.getAttribute("com.caucho.portal.error.status_code");

    Constraint constraint = event.getConstraint();
    Class constraintType = constraint.getClass();
    Integer statusCode = new Integer(event.getStatusCode());

    request.setAttribute( "com.caucho.portal.error.constraint", 
                          constraint );
    request.setAttribute( "com.caucho.portal.error.constraint_type",
                          constraintType);
    request.setAttribute( "com.caucho.portal.error.status_code", 
                          statusCode );

    try {
      if (handleErrorPage(request, response))
        event.setHandled(false);
      else
        event.setHandled(true);
    }
    finally {
      if (existingConstraint == null) {
        request.removeAttribute( "com.caucho.portal.error.constraint" );
        request.removeAttribute( "com.caucho.portal.error.constraint_type" );
        request.removeAttribute( "com.caucho.portal.error.status_code" );
      } 
      else {
        request.setAttribute( "com.caucho.portal.error.constraint", 
                              existingConstraint );

        request.setAttribute( "com.caucho.portal.error.constraint_type",
                              existingConstraintType );

        request.setAttribute( "com.caucho.portal.error.status_code",
                              existingStatusCode );
      }
    }
  }


  /**
   * {@inheritDoc}
   *
   * @see #setErrorPage(String)
   */ 
  public void handleException( RenderRequest request, 
                               RenderResponse response, 
                               ExceptionEvent event )
  {
    if (_errorPage == null)
      return;

    Object existingException = 
      request.getAttribute("com.caucho.portal.error.exception");

    Object existingExceptionType = 
      request.getAttribute("com.caucho.portal.error.exception_type");

    Object existingMessage = 
      request.getAttribute("com.caucho.portal.error.message");

    Exception exception = event.getException();
    Class exceptionType = exception.getClass();
    String message = exception.getMessage();

    request.setAttribute( "com.caucho.portal.error.exception",
                          exception );

    request.setAttribute( "com.caucho.portal.error.exception_type", 
                          exceptionType );

    request.setAttribute( "com.caucho.portal.error.message", 
                          message );

    try {
      if (handleErrorPage(request, response))
        event.setHandled(false);
      else
        event.setHandled(true);
    }
    finally {
      if (existingException == null) {
        request.removeAttribute( "com.caucho.portal.error.exception" );
        request.removeAttribute( "com.caucho.portal.error.exception_type" );
        request.removeAttribute( "com.caucho.portal.error.message" );
      } 
      else {
        request.setAttribute( "com.caucho.portal.error.exception", 
                              existingException );

        request.setAttribute( "com.caucho.portal.error.exception_type",
                              existingExceptionType );

        request.setAttribute( "com.caucho.portal.error.message",
                              existingMessage );
      }
    }
  }

  private boolean handleErrorPage( RenderRequest request, 
                                   RenderResponse response )
  {
    try {

      PortletRequestDispatcher dispatcher 
        = getPortletContext().getRequestDispatcher(_errorPage);

      dispatcher.include(request, response);
    }
    catch (Exception ex) {
      log.log(Level.WARNING, ex.toString(), ex);
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  public ResourceBundle getResourceBundle(Locale locale)
  {
    if (_resourceBundleFactory == null)
      _resourceBundleFactory = new ResourceBundleFactory();

    return _resourceBundleFactory.getResourceBundle(locale);
  }

  /**
   * {@inheritDoc}
   */
  public int getBufferSize()
  {
    return _bufferSize;
  }

  abstract public void processAction(PortletConnection connection)
    throws PortletException, IOException;

  abstract public void render(PortletConnection connection)
    throws PortletException, IOException;

  public void destroy()
  {
  }
}


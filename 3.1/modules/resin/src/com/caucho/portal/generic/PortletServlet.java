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

import javax.portlet.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/**
 * A servlet that uses a class implementing javax.portlet.Portlet.
 *
 * This servlet supports the following configuration items.
 * Items marked with a * can be set as init-param.
 *
 * <dl>
 * <dd>portal 
 * <dt>an instance of {@link Portal}, default is an instance of 
 *     {@link GenericPortal}. 
 * <dt>portal-class*
 * <dd>a class name, an alternative to <code>portal</code>
 * <dt>portal-ref*
 * <dd>an attribute name to use for a lookup for a {@link Portal} as
 *     an application attribute.  This is an alternative to using 
 *     <code>portal</code> and is useful when more than one portlet
 *     servlet share a Portal.
 * <dt>portlet
 * <dd>an instance of {@link javax.portlet.Portlet}, required
 * <dt>portlet-class*
 * <dd>a class name, an alternative to <code>portlet</code>
 * <dt>portlet-name*
 * <dd>a name for the value of 
 *     {@link javax.portlet.PortletConfig#getPortletName()}, 
 *     the default is ServletConfig.getServletName()
 * <dt>portlet-preferences 
 * <dd>Set the default preferences for the portlet 
 *     (see <a href="#portlet-preferences">"Portlet Preferences"</a>)
 * <dt>expiration-cache*
 * <dd>an integer, a cache time in seconds for the portlet, 0 (the default) 
 *     disables and -1 means forever
 * <dt>private*
 * <dd>"true" or "false", if true the response is marked as private for
 *     the client, no cache will share the response with other users
 * <dt>supported-locales*
 *     A comma-separated list of locales of the form "en-us" or "en_us', the
 *     default is to support all locales.
 * <dt>resource-bundle*
 * <dd>the name of a resource bundle, the portlet may use the resource bundle
 *     with {@link javax.portlet.PortletConfig#getResourceBundle(Locale)}
 * <dt>renderer
 * <dd>an instance of  {@link Renderer}, used to add decorations like headers
 *     footers and controls to the portlet, the default is no renderer. See 
 *     {@link AbstractRenderer}.
 * <dt>renderer-class*
 * <dd>a class name, an alternative to <code>renderer</code>
 *     {@link #setRenderer(Renderer)}.
 * </dl> 
 *
 * <h3><a name="portlet-preferences">Portlet Preferences</a></h3> 
 *
 * <pre> 
 * &lt;servlet servlet-name="portal" 
 *          servlet-class="com.caucho.portal.generic.PortletServlet"&gt; 
 *
 *   &lt;init&gt;
 *
 *     ... 
 *
 *     &lt;portlet-preferences&gt;
 *       &lt;preference name="colour" value="green" read-only="true"/&gt; 
 *         or
 *       &lt;preference&gt;
 *         &lt;name&gt;colour&lt;/name&gt;
 *         &lt;value&gt;green&lt;/value&gt;
 *         &lt;read-only&gt;true&lt;/read-only&gt;
 *       &lt;/preference&gt;
 *     &lt;/portlet-preferences&gt;
 *
 *     ... 
 *   &lt;/init&gt;
 *  &lt;/servlet&gt; 
 * </pre> 
 */
public class PortletServlet
  extends HttpServlet
  implements Window, PortletConfig
{
  static protected final Logger log = 
    Logger.getLogger(PortletServlet.class.getName());

  private Portal _portal;
  private HttpPortletContext _portletContext;

  private Portlet _portlet;
  private String _portletName;
  private String _namespace = "";
  private Map<String, String> _initParamMap;
  private int _expirationCache;
  private boolean _isPrivate;
  private Renderer _renderer;
  private int _bufferSize;
  private Set<Locale> _supportedLocales;
  private GenericPortletPreferences _defaultPreferences;
  private ResourceBundleFactory _resourceBundleFactory;

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
   * An alternative to {@link #setPortal(Portal)}, specify the name
   * of an attribute to lookup in the ServletContext.  This is useful
   * for sharing a Portal amongst different servlets.
   */
  public void setPortalRef(String attributeName)
  {
    Portal portal = (Portal) getServletContext().getAttribute(attributeName);

    if (portal == null)
      throw new IllegalArgumentException(
          "Portal not found with ServletContext attribute name `" 
          + attributeName + "'");

    setPortal(portal);
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
   * The portlet, required.  This method can be called in derived classes,
   * or through the use of depndency injection on containers that support it,
   * or indirectly using the init param `portlet-class'.
   */
  public void setPortlet(Portlet portlet)
  {
    if (_portlet != null)
      throw new IllegalArgumentException("`portlet' is already set");

    _portlet = portlet;
  }


  /**
   * An alternative to {@link #setPortlet(Portlet)}, specify the class
   * name of an object to instantiate
   */
  public void setPortletClass(String className)
  {
    setPortlet( (Portlet) newInstance(Portlet.class, className) );
  }

  /** 
   * The default is the value of ServletConfig.getServletName()
   */ 
  public void setPortletName(String portletName)
  {
    _portletName = portletName;
  }

  /**
   * Add an init-param for the portlet.  If no init-param are added, the
   * default behaviour is to expose the Servlet's init-param to the portlet.
   * If this method is called at least once, the Servlet's init-param are not
   * exposed to the portlet.
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
   *
   * Can also be specified with 
   * <a href="#init-param">init-param</a> `expiration-cache'. 
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
   *
   * Can also be specified with 
   * <a href="#init-param">init-param</a> `private'. 
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
   * Default is 0
   */
  public void setBufferSize(int bufferSize)
  {
    _bufferSize = bufferSize;
  }

  public void init(ServletConfig servletConfig)
    throws ServletException
  {
    super.init(servletConfig);

    String p;

    p = super.getInitParameter("portal-class");
    if (p != null)
      setPortalClass( p );

    p = super.getInitParameter("portal-ref");
    if (p != null)
      setPortalRef(p);

    if (_portal == null)
      _portal = new GenericPortal();

    p = super.getInitParameter("portlet-class");
    if (p != null)
      setPortletClass( p );

    if (_portlet == null)
      throw new ServletException("`portlet' is required");

    p = super.getInitParameter("portlet-name");
    if (p != null)
      setPortletName(p);

    if (_portletName == null)
      _portletName = servletConfig.getServletName();

    p = super.getInitParameter("expiration-cache");
    if (p != null)
      setExpirationCache(Integer.parseInt(p));

    p = super.getInitParameter("private");
    if (p != null)
      setPrivate(Boolean.valueOf(p).booleanValue());

    p = super.getInitParameter("buffer-size");
    if (p != null)
      setBufferSize(Integer.parseInt(p));

    p = super.getInitParameter("supported-locales");
    if (p != null)
      addSupportedLocales( p );

    p = super.getInitParameter("resource-bundle");
    if (p != null)
      setResourceBundle( p );

    p = super.getInitParameter("renderer-class");
    if (p != null)
      setRendererClass( p );

    _portletContext = new HttpPortletContext(getServletContext());

    try {
      _portlet.init(this);
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

  public PortletConfig getPortletConfig()
  {
    return this;
  }

  public String getInitParameter(String name)
  {
    if (_initParamMap == null)
      return super.getInitParameter(name);
    else
      return _initParamMap.get(name);
  }

  public Enumeration getInitParameterNames()
  {
    if (_initParamMap == null)
      return super.getInitParameterNames();
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
   * This implementation does nothing.
   */ 
  public void handleConstraintFailure(RenderRequest request, 
                                      RenderResponse response, 
                                      ConstraintFailureEvent event)
  {
  }


  /**
   * {@inheritDoc}
   *
   * This implementation does nothing. 
   */ 
  public void handleException(RenderRequest request, 
                              RenderResponse response, 
                              ExceptionEvent event)
  {
    // XXX: todo: support error-page
  }

  /**
   * {@inheritDoc}
   */
  public String getPortletName()
  {
    return _portletName;
  }

  /**
   * {@inheritDoc}
   */
  public PortletContext getPortletContext()
  {
    return _portletContext;
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
      Action action = connection.getAction(this, _namespace);

      if (action != null) {
        try {
          if (action.isTarget())  {
            action.processAction(_portlet);
          }
        } 
        finally { 
          action.finish(); 
        } 
      }

      Render render = connection.getRender(this, _namespace);

      if (render != null) {
        try {
          render.render(_portlet);
        } 
        finally { 
          render.finish(); 
        } 
      }

      connection.checkForFailure();
    }
    catch (PortletException ex) {
      throw new ServletException(ex);
    }
    finally {
      connection.finish();
    }
  }

  public void destroy()
  {
    if (_portlet != null)
      _portlet.destroy();
  }

}


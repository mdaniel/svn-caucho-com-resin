/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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

package com.caucho.v5.http.dispatch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.make.DependencyContainer;
import com.caucho.v5.servlets.FileServlet;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.Path;

/**
 * Manages dispatching: servlets and filters.
 */
public class ServletMapper {
  private static final Logger log = Logger.getLogger(ServletMapper.class.getName());
  private static final L10N L = new L10N(ServletMapper.class);

  private static final HashSet<String> _welcomeFileResourceMap
    = new HashSet<String>();

  private WebApp _webApp;

  private ServletManager _servletManager;

  private UrlMap<ServletMapping> _servletMap
    = new UrlMap<>();

  private HashMap<String,ServletMapping> _regexpMap
    = new HashMap<>();

  private ArrayList<String> _ignorePatterns = new ArrayList<String>();
  
  private ConcurrentArrayList<ServletDefaultMapper> _defaultMappers
    = new ConcurrentArrayList<>(ServletDefaultMapper.class);

  private String _defaultServlet;

  //Servlet 3.0 maps serletName to urlPattern
  private Map<String, Set<String>> _urlPatterns
    = new HashMap<String, Set<String>>();

  //Servlet 3.0 urlPattern to servletName
  private Map<String, ServletMapping> _servletNamesMap
    = new HashMap<String, ServletMapping>();

  public ServletMapper(WebApp webApp)
  {
    _webApp = webApp;
  }
  
  /**
   * Gets the servlet context.
   */
  public WebApp getWebApp()
  {
    return _webApp;
  }

  public void setServletContext(WebApp webApp)
  {
    _webApp = webApp;
  }

  /**
   * Returns the servlet manager.
   */
  public ServletManager getServletManager()
  {
    return _servletManager;
  }

  /**
   * Sets the servlet manager.
   */
  public void setServletManager(ServletManager manager)
  {
    _servletManager = manager;
  }

  /**
   * Adds a servlet mapping
   */
  public void addUrlRegexp(String regexp,
                           String servletName,
                           ServletMapping mapping)
    throws ServletException
  {
    _servletMap.addRegexp(regexp, mapping);
    _regexpMap.put(servletName, mapping);
  }

  /**
   * Adds a servlet mapping
   */
  void addUrlMapping(final String urlPattern,
                     String servletName,
                     ServletMapping mapping,
                     boolean ifAbsent)
    throws ServletException
  {
    try {
      boolean isIgnore = false;

      if (mapping.isInFragmentMode()
         && _servletMap.contains(new FragmentFilter(servletName)))
        return;

      if (servletName == null) {
        throw new ConfigException(L.l("servlets need a servlet-name."));
      }
      else if (servletName.equals("invoker")) {
        // special case
      }
      else if (servletName.equals("plugin_match")
               || servletName.equals("plugin-match")) {
        // special case
        isIgnore = true;
      }
      else if (servletName.equals("plugin_ignore")
               || servletName.equals("plugin-ignore")) {
        if (urlPattern != null)
          _ignorePatterns.add(urlPattern);

        return;
      }
      /*
      else if (mapping.getBean() != null) {
      }
      */
      else if (_servletManager.getServlet(servletName) == null)
        throw new ConfigException(L.l("'{0}' is an unknown servlet-name.  servlet-mapping requires that the named servlet be defined in a <servlet> configuration before the <servlet-mapping>.", servletName));

      if ("/".equals(urlPattern)) {
        _defaultServlet = servletName;
      }
      else if (mapping.isStrictMapping()) {
        _servletMap.addStrictMap(urlPattern, null, mapping);
      }
      else
        _servletMap.addMap(urlPattern, mapping, isIgnore, ifAbsent);
      
      Set<String> patterns = _urlPatterns.get(servletName);

      if (patterns == null) {
        patterns = new HashSet<String>();

        _urlPatterns.put(servletName, patterns);
      }

      _servletNamesMap.put(urlPattern, mapping);

      patterns.add(urlPattern);

      log.finer("servlet-mapping " + urlPattern + " -> " + servletName);
    } catch (ServletException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Set<String> getUrlPatterns(String servletName)
  {
    return _urlPatterns.get(servletName);
  }
  
  public void addDefaultMapper(ServletDefaultMapper mapper)
  {
    _defaultMappers.add(mapper);
  }

  /**
   * Sets the default servlet.
   */
  public void setDefaultServlet(String servletName)
    throws ServletException
  {
    _defaultServlet = servletName;
  }

  public FilterChain mapServlet(InvocationServlet invocation)
    throws ServletException
  {
    String contextURI = invocation.getContextURI();

    String servletName = null;
    ArrayList<String> vars = new ArrayList<String>();

    invocation.setClassLoader(Thread.currentThread().getContextClassLoader());

    ServletBuilder config = null;
    ServletMapping servletRegexp = null;
    
    if (_servletMap != null) {
      ServletMapping servletMap = _servletMap.map(contextURI, vars);

      if (servletMap != null && servletMap.isServletConfig())
        config = servletMap;

      if (servletMap != null) {
        servletRegexp = servletMap.initRegexpConfig(vars);
        
        if (servletRegexp != null) {
          try {
            servletRegexp.getServletClass();
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);

            return new FilterChainError(404);
          }
        }
      }

      if (servletRegexp != null) {
        servletName = servletRegexp.getServletName();
      }
      else if (servletMap != null) {
        servletName = servletMap.getServletName();
      }
    }

    if (servletName == null) {
      try {
        InputStream is;
        is = _webApp.getResourceAsStream(contextURI);

        if (is != null) {
          is.close();

          servletName = _defaultServlet;
        }
      } catch (Exception e) {
      }
    }

    MatchResult matchResult = null;

    if (matchResult == null && contextURI.endsWith("j_security_check")) {
      servletName = "j_security_check";
    }

    if (servletName == null) {
      // matchResult = matchWelcomeFileResource(invocation, vars);
      matchResult = null;

      if (matchResult != null)
        servletName = matchResult.getServletName();

      if (matchResult != null 
          && ! contextURI.endsWith("/")
          && ! (invocation instanceof SubInvocation)) {
        String contextPath = invocation.getContextPath();

        return new FilterChainRedirect(contextPath + contextURI + "/");
      }

      if (matchResult != null && invocation instanceof InvocationServlet) {
        InvocationServlet inv = (InvocationServlet) invocation;

        inv.setContextURI(matchResult.getContextUri());
        // server/10r9
        // inv.setRawURI(inv.getRawURI() + file);
      }
    }

    if (servletName == null)
      servletName = mapDefault(invocation);

    if (servletName == null) {
      servletName = _defaultServlet;
      vars.clear();

      if (matchResult != null)
        vars.add(matchResult.getContextUri());
      else
        vars.add(contextURI);

      addWelcomeFileDependency(invocation);
    }
    
    if (servletName == null) {
      log.fine(L.l("'{0}' has no default servlet defined", contextURI));

      return new FilterChainError(404);
    }

    String servletPath = vars.get(0);

    invocation.setServletPath(servletPath);

    if (servletPath.length() < contextURI.length())
      invocation.setPathInfo(contextURI.substring(servletPath.length()));
    else
      invocation.setPathInfo(null);

    if (servletRegexp != null)
      config = servletRegexp;

    if (servletName.equals("invoker"))
      servletName = handleInvoker(invocation);

    invocation.setServletName(servletName);

    if (log.isLoggable(Level.FINER)) {
      log.finer("map (uri:"
                + contextURI + " -> " + servletName + ") " + _webApp);
    }

    // server/13f1
    ServletBuilder newConfig = _servletManager.getServlet(servletName);

    if (newConfig != null) {
      config = newConfig;
    }

    if (config != null) {
      invocation.setSecurityRoleMap(config.getRoleMap());
    }

    FilterChain chain
      = _servletManager.createServletChain(servletName, config, invocation);

    return chain;
  }

  private String mapDefault(InvocationServlet invocation)
  {
    ServletDefaultMapper []mappers = _defaultMappers.toArray();

    int bestWeight = -1;
    String bestServlet = null;

    for (int i = 0; i < mappers.length; i++) {
      ServletDefaultMapper mapper = mappers[i];

      final int weight = mapper.weigh(invocation);

      if (weight > bestWeight) {
        bestWeight = weight;
        bestServlet = mapper.getServletName();
      }
    }

    return bestServlet;
  }

  private void addWelcomeFileDependency(Invocation servletInvocation)
  {
    if (! (servletInvocation instanceof InvocationServlet))
      return;

    InvocationServlet invocation = (InvocationServlet) servletInvocation;

    String contextURI = invocation.getContextURI();

    DependencyContainer dependencyList = new DependencyContainer();

    WebApp webApp = (WebApp) _webApp;

    String uriRealPath = webApp.getRealPath(contextURI);
    Path contextPath = webApp.getRootDirectory().lookup(uriRealPath);

    if (! contextPath.isDirectory())
      return;

    ArrayList<String> welcomeFileList = webApp.getBuilder().getWelcomeFileList();
    for (int i = 0; i < welcomeFileList.size(); i++) {
      String file = welcomeFileList.get(i);

      String realPath = webApp.getRealPath(contextURI + "/" + file);

      Path path = webApp.getRootDirectory().lookup(realPath);

      dependencyList.add(new Depend(path));
    }

    dependencyList.clearModified();

    invocation.setDependency(dependencyList);
  }

  private String handleInvoker(InvocationServlet invocation)
    throws ServletException
  {
    String tail;

    if (invocation.getPathInfo() != null)
      tail = invocation.getPathInfo();
    else
      tail = invocation.getServletPath();

      // XXX: this is really an unexpected, internal error that should never
      //      happen
    if (! tail.startsWith("/")) {
      throw new ConfigException("expected '/' starting " +
                                 " sp:" + invocation.getServletPath() +
                                 " pi:" + invocation.getPathInfo() +
                                 " sn:invocation" + invocation);
    }

    int next = tail.indexOf('/', 1);
    String servletName;

    if (next < 0)
      servletName = tail.substring(1);
    else
      servletName = tail.substring(1, next);

    // XXX: This should be generalized, possibly with invoker configuration
    if (servletName.startsWith("com.caucho")) {
      throw new ConfigException(L.l("servlet '{0}' forbidden from invoker. com.caucho.* classes must be defined explicitly in a <servlet> declaration.",
                                    servletName));
    }
    else if (servletName.equals("")) {
      throw new ConfigException(L.l("invoker needs a servlet name in URL '{0}'.",
                                    invocation.getContextURI()));
    }
    
    addServlet(servletName);

    String servletPath = invocation.getServletPath();
    if (invocation.getPathInfo() == null) {
    }
    else if (next < 0) {
      invocation.setServletPath(servletPath + tail);
      invocation.setPathInfo(null);
    }
    else if (next < tail.length()) {

      invocation.setServletPath(servletPath + tail.substring(0, next));
      invocation.setPathInfo(tail.substring(next));
    }
    else {
      invocation.setServletPath(servletPath + tail);
      invocation.setPathInfo(null);
    }

    return servletName;
  }

  public String getServletPattern(String uri)
  {
    ArrayList<String> vars = new ArrayList<String>();

    Object value = null;

    if (_servletMap != null)
      value = _servletMap.map(uri, vars);

    if (value != null)
      return uri;
    else
      return null;
  }
  
  public String getServletClassByUri(String uri)
  {
    ArrayList<String> vars = new ArrayList<String>();

    ServletMapping value = null;

    if (_servletMap != null)
      value = _servletMap.map(uri, vars);

    if (value != null) {
      Class<?> servletClass = value.getServletClass(vars);

      if (servletClass != null)
        return servletClass.getName();
      else {
        String servletName = value.getServletName();
        
        ServletBuilder config = _servletManager.getServlet(servletName);
        
        if (config != null)
          return config.getServletClassName();
        else
          return servletName;
      }
    }
    else
      return null;
  }

  /**
   * Returns the servlet matching patterns.
   */
  public ArrayList<String> getURLPatterns()
  {
    ArrayList<String> patterns = _servletMap.getURLPatterns();

    return patterns;
  }

  public ServletMapping getServletMapping(String pattern)
  {
    return _servletNamesMap.get(pattern);
  }

  public boolean containsServlet(String servletName)
  {
    for (ServletBuilder builder : _servletNamesMap.values()) {
      if (builder.getServletName().equals(servletName)) {
        return true;
      }
    }
    
    return false;
  }

  /**
   * Returns the servlet plugin_ignore patterns.
   */
  public ArrayList<String> getIgnorePatterns()
  {
    return _ignorePatterns;
  }

  private void addServlet(String servletName)
    throws ServletException
  {
    if (_servletManager.getServlet(servletName) != null)
      return;

    ServletBuilder config = new ServletBuilder();
    config.setServletContext(_webApp);
    config.setServletName(servletName);
    
    try {
      config.setServletClass(servletName);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }

    config.init();

    _servletManager.addServlet(config);
  }

  public void destroy()
  {
    _servletManager.destroy();
  }

  private class FragmentFilter implements UrlMap.Filter<ServletMapping> {
    private String _servletName;

    public FragmentFilter(String servletName)
    {
      _servletName = servletName;
    }

    @Override
    public boolean isMatch(ServletMapping item)
    {
      return _servletName.equals(item.getServletName());
    }
  }

  private static class MatchResult {
    String _servletName;
    String _contextUri;

    private MatchResult(String servletName, String contextUri)
    {
      _servletName = servletName;
      _contextUri = contextUri;
    }

    public String getServletName()
    {
      return _servletName;
    }

    public String getContextUri()
    {
      return _contextUri;
    }
  }

  static {
    _welcomeFileResourceMap.add(FileServlet.class.getName());
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.JspServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.JspXmlServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.quercus.servlet.QuercusServlet");
    _welcomeFileResourceMap.add("com.caucho.v5.jsp.XtpServlet");
  }
}



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

package com.caucho.v5.http.webapp;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.caucho.v5.bytecode.scan.ScanClass;
import com.caucho.v5.bytecode.scan.ScanClassBase;
import com.caucho.v5.bytecode.scan.ScanListenerByteCode;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.JarPath;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;
import com.caucho.v5.websocket.server.ServerContainerImpl;

/**
 * classpath scanning for web-app resources like @WebServlet
 */
public class WebAppBuilderScan
{
  private static final L10N L = new L10N(WebAppBuilderScan.class);
  
  private static final Logger log
    = Logger.getLogger(WebAppBuilderScan.class.getName());

  private static final char []SERVLET_ANNOTATION
    = "javax.servlet.annotation.".toCharArray();

  private static final char []WEBSOCKET_ANNOTATION
    = "javax.websocket.".toCharArray();

  private static final char []SERVER_APPLICATION_CONFIG
    = ServerApplicationConfig.class.getName().toCharArray();
 
  private List<String> _pendingClasses = new ArrayList<String>();
  private ScanListenerClassHierarchy _classHierarchyScanListener;
  
  private final WebAppBuilder _builder;
  
  /**
   * Builder Creates the webApp with its environment loader.
   */
  public WebAppBuilderScan(WebAppBuilder builder)
  {
    Objects.requireNonNull(builder);
    
    _builder = builder;
  }
  
  private WebAppBuilder getBuilder()
  {
    return _builder;
  }
  
  private WebApp getWebApp()
  {
    return getBuilder().getWebApp();
  }

  void addScanListeners()
  {
    // XXX: getWebApp().getClassLoader().addScanListener(new WebFragmentScanner());
  }

  void loadInitializers()
    throws Exception
  {
    // XXX: websockets forces this
    _classHierarchyScanListener = new ScanListenerClassHierarchy(getWebApp().getClassLoader());
    
    // XXX: getWebApp().getClassLoader().addScanListener(_classHierarchyScanListener);

    Class<?> cl = ServletContainerInitializer.class;
    
    Enumeration<URL> e;
    e = getWebApp().getClassLoader().getResources("META-INF/services/" + cl.getName());
    
    if (e == null) {
      return;
    }
    
    while (e.hasMoreElements()) {
      URL url = e.nextElement();

      String fullPath = url.toString();
      
      if (fullPath.startsWith("jar:")) {
        int p = fullPath.indexOf('!');
        
        if (p > 0) {
          fullPath = fullPath.substring("jar:".length(), p);
        }
      }
      
      //Path rootPath = Vfs.lookup(fullPath);
      
      // might parse to check that the loader has a handles
      if (_classHierarchyScanListener == null) {
        _classHierarchyScanListener = new ScanListenerClassHierarchy(getWebApp().getClassLoader());
        
        // XXX: getWebApp().getClassLoader().addScanListener(_classHierarchyScanListener);
      }
      
      //_classHierarchyScanListener.addRoot(rootPath);
    }
  }

  void callInitializers()
    throws Exception
  {
    List<ServletContainerInitializer> initList
      = new ArrayList<>(loadLocalServices(ServletContainerInitializer.class));
    
    //initList.add(new ServletContainerInitBaratine());
    
    Collections.sort(initList, new InitComparator());

    for (ServletContainerInitializer init : initList) {
      callInitializer(init);
    }
    
    _classHierarchyScanListener = null;
  }
  
  private <X> List<X> loadLocalServices(Class<X> cl)
  {
    ArrayList<X> list = new ArrayList<>();
    
    for (X loader : ServiceLoader.load(cl)) {
      list.add(loader);
    }
    
    return list;
  }
  
  private PathImpl getRootPath(PathImpl path, String name)
  {
    if (path instanceof JarPath) {
      return ((JarPath) path).getContainer();
    }
    else {
      String url = path.getURL();

      return path.lookup(url.substring(0, url.length() - name.length()));
    }
  }

  private class InitComparator
    implements Comparator<ServletContainerInitializer>
  {
    @Override
    public int compare(ServletContainerInitializer a,
                       ServletContainerInitializer b)
    {
      ClassLoader loader = getWebApp().getClassLoader();

      try {
        String aName = a.getClass().getName().replace('.', '/') + ".class";
        String bName = b.getClass().getName().replace('.', '/') + ".class";

        URL aUrl = loader.getResource(aName);
        URL bUrl = loader.getResource(bName);

        if (aUrl == null && bUrl == null) {
          return 0;
        }

        if (aUrl == null && bUrl != null) {
          return 1;
        }

        if (aUrl != null && bUrl == null) {
          return -1;
        }

        PathImpl aPath = VfsOld.lookup(aUrl.toString());
        PathImpl bPath = VfsOld.lookup(bUrl.toString());

        PathImpl aRoot = getRootPath(aPath, aName);
        PathImpl bRoot = getRootPath(bPath, bName);

        int aIndex = fragmentIndexOf(aRoot);
        int bIndex = fragmentIndexOf(bRoot);

        return Integer.signum(aIndex - bIndex);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
        return 0;
      }
    }

    private int fragmentIndexOf(PathImpl root)
    {
      List<WebAppFragmentConfig> fragments = _builder.getBuilderFragment().getFragments();
      
      for (int i = 0; i < fragments.size(); i++) {
        WebAppFragmentConfig fragment = fragments.get(i);

        if (root.equals(fragment.getRootPath())) {
          return i;
        }
      }
      
      return -1;
    }
  }

  private void callInitializer(ServletContainerInitializer init)
    throws ServletException
  {
    HandlesTypes handlesTypes
      = init.getClass().getAnnotation(HandlesTypes.class);

    if (handlesTypes == null) {
      if (log.isLoggable(Level.FINER)){
        log.finer("ServletContainerInitializer " + init + " {in " + this + "}");
      }
      init.onStartup(null, getWebApp());
      return;
    }
    
    if (_classHierarchyScanListener == null) {
      return;
    }
    
    HashSet<Class<?>> classes 
       = _classHierarchyScanListener.findClasses(handlesTypes.value());

    if (classes != null) {
      if (log.isLoggable(Level.FINER)){
        log.finer("ServletContainerInitializer " + init + "(" + classes + ") {in " + this + "}");
      }
      
      init.onStartup(classes, getWebApp());
    }
  }

  public void initAnnotated() throws Exception
  {
    List<Class<?>> listeners = new ArrayList<Class<?>>();

    List<Class<? extends Servlet>> servlets
      = new ArrayList<Class<? extends Servlet>>();

    List<Class<?>> filters = new ArrayList<>();

    List<String> pendingClasses = new ArrayList<>();
    
    List<Class<?>> wsEndpoints = new ArrayList<>();
    List<Class<?>> wsApplications = new ArrayList<>();
    
    // server/121e
    if (! getBuilder().isMetadataComplete()) {
      pendingClasses = new ArrayList<String>(_pendingClasses);
      _pendingClasses.clear();
    }
    for (String className : pendingClasses) {
      Class<?> cl = getWebApp().getClassLoader().loadClass(className);
      
      if (ServletContextListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletContextAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletRequestListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (ServletRequestAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (HttpSessionListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (HttpSessionAttributeListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (HttpSessionIdListener.class.isAssignableFrom(cl))
        listeners.add(cl);
      else if (Servlet.class.isAssignableFrom(cl))
        servlets.add((Class) cl);
      else if (Filter.class.isAssignableFrom(cl))
        filters.add(cl);
      else if (ServerApplicationConfig.class.isAssignableFrom(cl)) {
        wsApplications.add(cl);
      }
      else {
        if (cl.isAnnotationPresent(ServerEndpoint.class)) {
          wsEndpoints.add(cl);
        }
      }
    }

    // server/12t7
    for (Class<?> listenerClass : listeners) {
      WebListener webListener
        = listenerClass.getAnnotation(WebListener.class);

      if (webListener == null) {
        continue;
      }

      ListenerConfig listener = new ListenerConfig();
      listener.setListenerClass(listenerClass);

      _builder.addListener(listener);
    }

    Collections.sort(filters, new ClassComparator());
    for (Class<?> filterClass : filters) {
      WebFilter webFilter
        = filterClass.getAnnotation(WebFilter.class);

      if (webFilter != null) {
        getBuilder().addFilter(webFilter, filterClass.getName());
      }
    }

    for (Class<? extends Servlet> servletClass : servlets) {
      WebServlet webServlet
        = servletClass.getAnnotation(WebServlet.class);

      if (webServlet != null) {
        getWebApp().getDispatcher().addServlet(webServlet, servletClass.getName());
      }

      ServletSecurity servletSecurity
        = (ServletSecurity) servletClass.getAnnotation(ServletSecurity.class);

      if (servletSecurity != null)
        getWebApp().getDispatcher().addServletSecurity(servletClass, servletSecurity);
    }
    
    for (Class<?> wsApplication : wsApplications) {
      addWebsocketApplication(wsApplication);
    }

    if (wsApplications.size() == 0) {
      // presence of ServerApplicationConfig disables other scanning
      for (Class<?> wsEndpoint : wsEndpoints) {
        addWebsocketEndpoint(wsEndpoint);
      }
    }
  }
  
  void addWebsocketEndpoint(Class<?> cl)
  {
    new ServerContainerImpl().addEndpoint(cl);
  }
  
  void addWebsocketApplication(Class<?> cl)
  {
    try {
      ServerApplicationConfig appConfig
        = (ServerApplicationConfig) cl.newInstance();
      
      HashSet<Class<?>> scanned = new HashSet<>();
      
      for (Class<?> scannedClass : appConfig.getAnnotatedEndpointClasses(scanned)) {
        Objects.requireNonNull(scannedClass);
        
        new ServerContainerImpl().addEndpoint(scannedClass);
      }
      
      HashSet<Class<? extends Endpoint>> scannedEnd = new HashSet<>();

      Set<ServerEndpointConfig> endpointConfigs = appConfig.getEndpointConfigs(scannedEnd);
      
      if (endpointConfigs != null) {
        for (ServerEndpointConfig cfg : endpointConfigs) {
          Objects.requireNonNull(cfg);
        
          new ServerContainerImpl().addEndpoint(cfg);
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isAttributeListener(Class<?> cl)
  {
    if (ServletContextAttributeListener.class.isAssignableFrom(cl))
      return true;
    else
      return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getWebApp().getId() + "]";
  }

  class WebFragmentScanner implements ScanListenerByteCode {
    public int getScanPriority()
    {
      return 2;
    }

    @Override
    public boolean isRootScannable(PathImpl root, String packageRoot)
    {
      return true;
    }

    @Override
    public ScanClass scanClass(PathImpl root, String packageRoot,
                               String name, int modifiers)
    {
      if (Modifier.isPublic(modifiers))
        return new WebScanClass(name);
      else
        return null;
    }

    @Override
    public boolean isScanMatchAnnotation(StringBuilder sb)
    {
      String string = sb.toString();
      
      if (string.startsWith("javax.servlet.annotation.")) {
        return true;
      }
      
      if (string.startsWith("javax.websocket.")) {
        return true;
      }

      return false;
    }

    @Override
    public void classMatchEvent(ClassLoader loader,
                                PathImpl root,
                                String className)
    {
      _pendingClasses.add(className);
    }

    @Override
    public void completePath(PathImpl root)
    {
    }
  }

  class WebScanClass extends ScanClassBase
  {
    private String _className;
    private boolean _isValid;
    
    WebScanClass(String className)
    {
      _className = className;
    }
    
    @Override
    public void addClassAnnotation(char [] buffer, int offset, int length)
    {
      if (! isMatchBuffer(buffer, offset, length, SERVLET_ANNOTATION)
          && ! isMatchBuffer(buffer, offset, length, WEBSOCKET_ANNOTATION)) {
        return;
      }

      _isValid = true;
    }

    /**
     * Adds interface information to the scan class.
     */
    @Override
    public void addInterface(char[] buffer, int offset, int length)
    {
      if (isMatch(buffer, offset, length, SERVER_APPLICATION_CONFIG)) {
        _isValid = true;
      }
    }

    private final boolean isMatchBuffer(char []buffer, int offset, int length,
                                  char []match)
    {
      if (length < match.length)
        return false;
      
      for (int i = match.length - 1; i >= 0; i--) {
        if (buffer[offset + i] != match[i])
          return false;
      }
      
      return true;
    }

    /**
     * Complete scan processing.
     */
    @Override
    public boolean finishScan()
    {
      if (_isValid) {
        _pendingClasses.add(_className);
        return true;
      }
      else
        return false;
    }

    @Override
    public String toString()
    {
      return WebScanClass.class.getSimpleName() + "[" + _className + "]";
    }
  }
  
  private static class ClassComparator implements Comparator<Class<?>>
  {
    @Override
    public int compare(Class<?> a, Class<?> b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}

/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
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

package com.caucho.server.dispatch;

import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.config.types.InitParam;
import com.caucho.config.types.InitProgram;

import com.caucho.config.j2ee.InjectIntrospector;

import com.caucho.jmx.Jmx;

import com.caucho.jsp.QServlet;
import com.caucho.jsp.Page;

import com.caucho.server.connection.StubServletRequest;
import com.caucho.server.connection.StubServletResponse;

import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.Log;

/**
 * Configuration for a servlet.
 */
public class ServletConfigImpl implements ServletConfig, AlarmListener {
  static L10N L = new L10N(ServletConfigImpl.class);
  protected static final Logger log = Log.open(ServletConfigImpl.class);

  private String _location;
  
  private String _servletName;
  private String _servletClassName;
  private Class _servletClass;
  private String _jspFile;
  private String _displayName;
  private int _loadOnStartup = Integer.MIN_VALUE;

  private boolean _allowEL = true;
  private HashMap<String,String> _initParams = new HashMap<String,String>();

  private HashMap<String,String> _roleMap;

  private InitProgram _init;
  
  private RunAt _runAt;
  private Alarm _alarm;

  private ServletContext _servletContext;
  private ServletManager _servletManager;

  private ServletException _initException;
  private long _nextInitTime;
  
  private Servlet _servlet;
  private FilterChain _servletChain;
  
  /**
   * Creates a new servlet configuration object.
   */
  public ServletConfigImpl()
  {
  }

  /**
   * Sets the config location.
   */
  public void setConfigLocation(String location, int line)
  {
    _location = location + ":" + line + ": ";
  }

  /**
   * Sets the id attribute
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the servlet name.
   */
  public void setServletName(String name)
  {
    _servletName = name;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Gets the servlet name.
   */
  public String getServletClassName()
  {
    return _servletClassName;
  }

  /**
   * Sets the servlet class.
   */
  public void setServletClass(String servletClassName)
    throws ServletException
  {
    _servletClassName = servletClassName;
  }

  /**
   * Gets the servlet name.
   */
  public Class getServletClass()
  {
    return _servletClass;
  }

  /**
   * Sets the JSP file
   */
  public void setJspFile(String jspFile)
  {
    _jspFile = jspFile;
  }

  /**
   * Gets the JSP file
   */
  public String getJspFile()
  {
    return _jspFile;
  }

  /**
   * Sets the allow value.
   */
  public void setAllowEL(boolean allowEL)
  {
    _allowEL = allowEL;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(String param, String value)
  {
    _initParams.put(param, value);
  }

  /**
   * Sets an init-param
   */
  public InitParam createInitParam()
  {
    InitParam initParam = new InitParam();

    initParam.setAllowEL(_allowEL);

    return initParam;
  }

  /**
   * Sets an init-param
   */
  public void setInitParam(InitParam initParam)
  {
    _initParams.putAll(initParam.getParameters());
  }

  /**
   * Gets the init params
   */
  public Map getInitParamMap()
  {
    return _initParams;
  }

  /**
   * Gets the init params
   */
  public String getInitParameter(String name)
  {
    return _initParams.get(name);
  }

  /**
   * Gets the init params
   */
  public Enumeration getInitParameterNames()
  {
    return Collections.enumeration(_initParams.keySet());
  }

  /**
   * Returns the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext app)
  {
    _servletContext = app;
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
   * Sets the init block
   */
  public void setInit(InitProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public InitProgram getInit()
  {
    return _init;
  }

  /**
   * Sets the load-on-startup
   */
  public void setLoadOnStartup(int loadOnStartup)
  {
    _loadOnStartup = loadOnStartup;
  }

  /**
   * Gets the load-on-startup value.
   */
  public int getLoadOnStartup()
  {
    if (_loadOnStartup > Integer.MIN_VALUE)
      return _loadOnStartup;
    else if (_runAt != null)
      return 0;
    else
      return Integer.MIN_VALUE;
  }

  /**
   * Creates the run-at configuration.
   */
  public RunAt createRunAt()
  {
    if (_runAt == null)
      _runAt = new RunAt();

    return _runAt;
  }

  /**
   * Returns the run-at configuration.
   */
  public RunAt getRunAt()
  {
    return _runAt;
  }

  /**
   * Adds a security role reference.
   */
  public void addSecurityRoleRef(SecurityRoleRef ref)
  {
    if (_roleMap == null)
      _roleMap = new HashMap<String,String>();

    _roleMap.put(ref.getRoleName(), ref.getRoleLink());
  }

  /**
   * Adds a security role reference.
   */
  public HashMap<String,String> getRoleMap()
  {
    return _roleMap;
  }

  /**
   * Sets the display name
   */
  public void setDisplayName(String displayName)
  {
    _displayName = displayName;
  }

  /**
   * Gets the display name
   */
  public String getDisplayName()
  {
    return _displayName;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the icon
   */
  public void setIcon(com.caucho.config.types.Icon icon)
  {
  }

  /**
   * Sets the init exception
   */
  public void setInitException(ServletException exn)
  {
    _initException = exn;
    
    _nextInitTime = Long.MAX_VALUE / 2;

    if (exn instanceof UnavailableException) {
      UnavailableException unExn = (UnavailableException) exn;
      
      if (! unExn.isPermanent())
        _nextInitTime = (Alarm.getCurrentTime() +
                         1000L * unExn.getUnavailableSeconds());
    }
  }

  /**
   * Returns the servlet.
   */
  public Servlet getServlet()
  {
    return _servlet;
  }

  /**
   * Initialize the servlet config.
   */
  public void init()
    throws ServletException
  {
    if (_runAt != null) {
      _alarm = new Alarm(this);
    }

    if (_servletName == null)
      _servletName = _servletClassName;
  }

  protected void validateClass(boolean requireClass)
    throws ServletException
  {
    if (_runAt != null || _loadOnStartup >= 0)
      requireClass = true;
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    if (_servletClassName == null) {
    }
    else if (_servletClassName.equals("invoker")) {
    }
    else {
      try {
        _servletClass = Class.forName(_servletClassName, false, loader);
      } catch (ClassNotFoundException e) {
	log.log(Level.FINER, e.toString(), e);
      }

      if (_servletClass != null) {
      }
      else if (requireClass) {
        throw error(L.l("`{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
      }
      else {
	String location = _location != null ? _location : "";
	  
        log.warning(L.l(location + "`{0}' is not a known servlet.  Servlets belong in the classpath, often in WEB-INF/classes.", _servletClassName));
	return;
      }

      if (! Servlet.class.isAssignableFrom(_servletClass))
        throw error(L.l("`{0}' must implement javax.servlet.Servlet.  All servlets must implement the Servlet interface.", _servletClassName));
      if (Modifier.isAbstract(_servletClass.getModifiers()))
        throw error(L.l("`{0}' must not be abstract.  Servlets must be fully-implemented classes.", _servletClassName));
      
      if (! Modifier.isPublic(_servletClass.getModifiers()))
        throw error(L.l("`{0}' must be public.  Servlets must be public classes.", _servletClassName));

      checkConstructor();
    }
  }

  /**
   * Checks the class constructor for the public-zero arg.
   */
  public void checkConstructor()
    throws ServletException
  {
    Constructor []constructors = _servletClass.getDeclaredConstructors();

    Constructor zeroArg = null;
    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
	zeroArg = constructors[i];
	break;
      }
    }

    if (zeroArg == null)
      throw error(L.l("`{0}' must have a zero arg constructor.  Servlets must have public zero-arg constructors.\n{1} is not a valid constructor.", _servletClassName, constructors[0]));

      
    if (! Modifier.isPublic(zeroArg.getModifiers()))
        throw error(L.l("`{0}' must be public.  '{1}' must have a public, zero-arg constructor.",
					     zeroArg,
					     _servletClassName));
  }

  /**
   * Handles a cron alarm callback.
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      log.fine(this + " cron");
      
      FilterChain chain = createServletChain();
      
      ServletRequest req = new StubServletRequest();
      ServletResponse res = new StubServletResponse();

      chain.doFilter(req, res);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      long nextTime = _runAt.getNextTimeout(Alarm.getCurrentTime());
      _alarm.queue(nextTime - Alarm.getCurrentTime());
    }
  }

  synchronized FilterChain createServletChain()
    throws ServletException
  {
    if (_servletChain != null)
      return _servletChain;

    String jspFile = getJspFile();
    FilterChain servletChain = null;

    if (jspFile != null) {
      QServlet jsp = (QServlet) _servletManager.createServlet("resin-jsp");

      servletChain = new PageFilterChain(_servletContext, jsp, jspFile, this);

      return servletChain;
    }

    validateClass(true);
    
    Class servletClass = getServletClass();

    if (servletClass == null) {
      throw new IllegalStateException(L.l("servlet class for {0} can't be null",
					  getServletName()));
    }
    else if (QServlet.class.isAssignableFrom(servletClass)) {
      servletChain = new PageFilterChain(_servletContext, (QServlet) createServlet());
    }
    else if (SingleThreadModel.class.isAssignableFrom(servletClass)) {
      servletChain = new SingleThreadServletFilterChain(this);
    }
    else {
      servletChain = new ServletFilterChain(this);
    }

    if (_roleMap != null)
      servletChain = new SecurityRoleMapFilterChain(servletChain, _roleMap);

    return servletChain;
  }
  
  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  synchronized Servlet createServlet()
    throws ServletException
  {
    if (_servlet != null)
      return _servlet;

    if (Alarm.getCurrentTime() < _nextInitTime)
      throw _initException;

    Class servletClass = getServletClass();

    if (log.isLoggable(Level.FINE))
      log.fine("Servlet[" + _servletName + "] starting");
    
    try {
      Servlet servlet = null;
      
      if (_jspFile != null) {
        servlet = createJspServlet(_servletName, _jspFile);

	if (servlet == null)
	  throw new ServletException(L.l("'{0}' is a missing JSP file.",
					 _jspFile));
      }

      else if (servletClass != null)
        servlet = (Servlet) servletClass.newInstance();
      
      else
        throw new ServletException(L.l("Null servlet class for `{0}'.",
                                       _servletName));

      InjectIntrospector.configure(servlet);

      // Initialize bean properties
      InitProgram init = getInit();

      if (init != null)
        init.getBuilderProgram().configure(servlet);

      try {
        servlet.init(this);
      } catch (UnavailableException e) {
        setInitException(e);
        throw e;
      }

      _servlet = servlet;

      // If the servlet has an MBean, register it
      try {
        Hashtable<String,String> props = new Hashtable<String,String>();
        props.put("type", "Servlet");
        props.put("name", _servletName);
        Jmx.register(_servlet, props);
      } catch (Throwable e) {
        log.finest(e.toString());
      }

      if (_runAt != null && _alarm != null) {
	long nextTime = _runAt.getNextTimeout(Alarm.getCurrentTime());
	_alarm.queue(nextTime - Alarm.getCurrentTime());
      }
      
      if (log.isLoggable(Level.FINER))
        log.finer("Servlet[" + _servletName + "] started");
        
      return _servlet;
    } catch (ServletException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      throw new ServletException(e);
    }
  }
  
  /**
   * Instantiates a servlet given its configuration.
   *
   * @param servletName the servlet
   *
   * @return the initialized servlet.
   */
  Servlet createJspServlet(String servletName, String jspFile)
    throws ServletException
  {
    try {
      ServletConfigImpl jspConfig = _servletManager.getServlet("resin-jsp");

      QServlet jsp = (QServlet) jspConfig.createServlet();

      Page page = jsp.getPage(servletName, jspFile);
    
      return page;
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  void killServlet()
  {
    Servlet servlet = _servlet;
    _servlet = null;

    if (_alarm != null)
      _alarm.dequeue();

    if (servlet != null)
      servlet.destroy();
  }

  public void close()
  {
    killServlet();

    _alarm = null;
  }

  protected ServletException error(String msg)
  {
    if (_location != null)
      return new ServletLineConfigException(_location + msg);
    else
      return new ServletConfigException(msg);
  }

  /**
   * Returns a printable representation of the servlet config object.
   */
  public String toString()
  {
    return "ServletConfigImpl[name=" + _servletName + ",class=" + _servletClass + "]";
  }
}

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

package com.caucho.v5.resin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.InitParam;
import com.caucho.v5.http.dispatch.FilterConfigImpl;
import com.caucho.v5.http.dispatch.FilterMapping;
import com.caucho.v5.http.dispatch.ServletBuilderResin;
import com.caucho.v5.http.dispatch.ServletMapping;
import com.caucho.v5.http.webapp.DeployGeneratorWebAppSingle;
import com.caucho.v5.http.webapp.ListenerConfig;
import com.caucho.v5.http.webapp.WebApp;

/**
 * Embeddable version of a Resin web-app.
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/resin/foo");
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class WebAppEmbed
{
  private String _contextPath = "/";
  private String _rootDirectory = ".";
  private String _archivePath;
  
  private boolean _isStartDisabled;

  private HashMap<String,String> _contextParamMap
    = new HashMap<String,String>();
  
  private final ArrayList<BeanEmbed> _beanList
    = new ArrayList<BeanEmbed>();

  private final ArrayList<ServletEmbed> _servletList
    = new ArrayList<ServletEmbed>();

  private final ArrayList<ServletMappingEmbed> _servletMappingList
    = new ArrayList<ServletMappingEmbed>();

  private final ArrayList<FilterEmbed> _filterList
    = new ArrayList<FilterEmbed>();

  private final ArrayList<FilterMappingEmbed> _filterMappingList
    = new ArrayList<FilterMappingEmbed>();

  private final ArrayList<ListenerEmbed> _listenerList
    = new ArrayList<ListenerEmbed>();
  
  private WebApp _webApp;
  
  private DeployGeneratorWebAppSingle _deployGenerator;
  
  /**
   * Creates a new embedded webapp
   */
  public WebAppEmbed()
  {
  }

  /**
   * Creates a new embedded webapp
   *
   * @param contextPath the URL prefix of the web-app
   */
  public WebAppEmbed(String contextPath)
  {
    setContextPath(contextPath);
  }

  /**
   * Creates a new embedded webapp
   *
   * @param contextPath the URL prefix of the web-app
   * @param rootDirectory the root directory of the web-app
   */
  public WebAppEmbed(String contextPath, String rootDirectory)
  {
    setContextPath(contextPath);
    setRootDirectory(rootDirectory);
  }

  /**
   * The context-path
   */
  public void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * The context-path
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  /**
   * The root directory of the expanded web-app
   */
  public void setRootDirectory(String rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * The root directory of the expanded web-app
   */
  public String getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * The path to the archive war file
   */
  public void setArchivePath(String archivePath)
  {
    _archivePath = archivePath;
  }

  /**
   * The path to the archive war file
   */
  public String getArchivePath()
  {
    return _archivePath;
  }
  
  public void setDeployGenerator(DeployGeneratorWebAppSingle deployGenerator)
  {
    _deployGenerator = deployGenerator;
  }
  
  public DeployGeneratorWebAppSingle getDeployGenerator()
  {
    return _deployGenerator;
  }
  
  public ServletEmbed createServlet(Class<?> servletClass, String servletName)
  {
    ServletEmbed servletEmbed = new ServletEmbed(servletClass.getName(), servletName);
    
    addServlet(servletEmbed);
    
    return servletEmbed;
  }

  /**
   * Adds a servlet definition
   */
  public void addServlet(ServletEmbed servlet)
  {
    if (servlet == null)
      throw new NullPointerException();
    
    _servletList.add(servlet);
  }

  /**
   * Adds a servlet-mapping definition
   */
  public void addServletMapping(ServletMappingEmbed servletMapping)
  {
    if (servletMapping == null)
      throw new NullPointerException();
    
    _servletMappingList.add(servletMapping);
  }

  /**
   * Adds a filter definition
   */
  public void addFilter(FilterEmbed filter)
  {
    if (filter == null)
      throw new NullPointerException();
    
    _filterList.add(filter);
  }

  /**
   * Adds a filter-mapping definition
   */
  public void addFilterMapping(FilterMappingEmbed filterMapping)
  {
    if (filterMapping == null)
      throw new NullPointerException();
    
    _filterMappingList.add(filterMapping);
  }
  
  /**
   * Adds a listener
   */
  public WebAppEmbed addListener(Class<?> listenerClass)
  {
    _listenerList.add(new ListenerEmbed(listenerClass));
    
    return this;
  }

  /**
   * Adds a web bean.
   */
  public void addBean(BeanEmbed bean)
  {
    _beanList.add(bean);
  }

  /**
   * Sets a context-param.
   */
  public void setContextParam(String name, String value)
  {
    _contextParamMap.put(name, value);
  }
  
  /**
   * For cases like JSP compilation, skip the startup step.
   */
  public void setDisableStart(boolean isDisable)
  {
    _isStartDisabled = isDisable;
  }
  
  public WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Configures the web-app (for internal use)
   */
  protected void configure(WebApp webApp)
  {
    _webApp = webApp;
    
    webApp.setDisableStart(_isStartDisabled);
    
    try {
      for (Map.Entry<String,String> entry : _contextParamMap.entrySet()) {
        InitParam initParam = new InitParam(entry.getKey(), entry.getValue());
        webApp.getBuilder().addContextParam(initParam);
      }

      for (BeanEmbed beanEmbed : _beanList) {
        beanEmbed.configure();
      }

      for (ServletEmbed servletEmbed : _servletList) {
        ServletBuilderResin servlet = (ServletBuilderResin) webApp.getBuilder().createServlet();

        servletEmbed.configure(servlet);

        webApp.getBuilder().addServlet(servlet);
      }
    
      for (ServletMappingEmbed servletMappingEmbed : _servletMappingList) {
        ServletMapping servletMapping = webApp.getBuilder().createServletMapping();

        servletMappingEmbed.configure(servletMapping);

        webApp.getBuilder().addServletMapping(servletMapping);
      }

      for (FilterEmbed filterEmbed : _filterList) {
        FilterConfigImpl filter = new FilterConfigImpl();

        filterEmbed.configure(filter);

        webApp.getBuilder().addFilter(filter);
      }
    
      for (FilterMappingEmbed filterMappingEmbed : _filterMappingList) {
        FilterMapping filterMapping = new FilterMapping();

        filterMappingEmbed.configure(filterMapping);

        webApp.getBuilder().addFilterMapping(filterMapping);
      }

      for (ListenerEmbed listenerEmbed : _listenerList) {
        ListenerConfig<?> config = new ListenerConfig();
        
        config.setListenerClass((Class) listenerEmbed.getListenerClass());

        webApp.addListener(config);
      }
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}

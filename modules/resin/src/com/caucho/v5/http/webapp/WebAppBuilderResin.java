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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.j2ee.PersistenceContextRefConfig;
import com.caucho.v5.config.types.EjbLocalRef;
import com.caucho.v5.config.types.EjbRef;
import com.caucho.v5.config.types.ResourceRef;
import com.caucho.v5.env.jpa.PersistenceManager;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.http.dispatch.ServletBuilderResin;
import com.caucho.v5.http.dispatch.ServletManager;
import com.caucho.v5.http.dispatch.ServletManagerResin;
import com.caucho.v5.http.rewrite.DispatchRuleBase;
import com.caucho.v5.http.rewrite.RewriteDispatch;
import com.caucho.v5.http.rewrite.RewriteFilter;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.http.webapp.WebAppResinBuilder;
import com.caucho.v5.http.webapp.WebAppController;
import com.caucho.v5.jsp.JspServlet;
import com.caucho.v5.jsp.cfg.JspConfig;
import com.caucho.v5.jsp.cfg.JspPropertyGroup;
import com.caucho.v5.jsp.cfg.JspTaglib;
import com.caucho.v5.jsp.el.JspApplicationContextImpl;
import com.caucho.v5.naming.JndiUtil;
import com.caucho.v5.security.RoleMapManager;
import com.caucho.v5.security.XmlRoleMap;


/**
 * Builder for the webapp to encapsulate the configuration process.
 */
public class WebAppBuilderResin extends WebAppResinBuilder<WebAppResin>
{
  private static final Logger log
    = Logger.getLogger(WebAppBuilderResin.class.getName());
  
  /*
  // List of default ear webApp configurations
  private ArrayList<EarConfig> _earDefaultList
    = new ArrayList<EarConfig>();

  private DeployContainerImpl<EnterpriseApplication,EarDeployController> _earDeploy;
  */

  private boolean _isSecure;

  private final JspPropertyGroup _jsp = new JspPropertyGroup();
  private JspConfig _jspConfig;
  private JspConfigDescriptorImpl _jspConfigDescriptor;

  private JspApplicationContextImpl _jspApplicationContext;
  // special
  private int _jspState;

  private ArrayList<JspTaglib> _taglibList;
  
  // private HashMap<String,Object> _extensions = new HashMap<>();

  //Rest

  // private RestScanner _restScanner;
  // private RestContainer _restContainer;

  private RewriteDispatch _requestRewriteDispatch;

  private RewriteDispatch _includeRewriteDispatch;

  private RewriteDispatch _forwardRewriteDispatch;

  /**
   * Builder Creates the webApp with its environment loader.
   */
  public WebAppBuilderResin(WebAppController controller)
  {
    super(controller);
  }
  
  protected ServletManager createServletManager()
  {
    return new ServletManagerResin();
  }

  public RewriteDispatch getRequestRewriteDispatch()
  {
    return _requestRewriteDispatch;
  }

  public RewriteDispatch getIncludeRewriteDispatch()
  {
    return _includeRewriteDispatch;
  }

  public RewriteDispatch getForwardRewriteDispatch()
  {
    return _forwardRewriteDispatch;
  }

  /**
   * Adds an enterprise webApp.
   */
  /*
  public void addApplication(EarConfig config)
  {
    DeployGenerator<EnterpriseApplication,EarDeployController> deploy
      = new EarDeployGeneratorSingle(_earDeploy, getWebApp(), config);

    _earDeploy.add(deploy);
  }
  */

  private void clearCache()
  {
  }

  /**
   * Updates an ear deploy
   */
  /*
  public void updateEarDeploy(String name)
    throws Throwable
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null) {
      entry.start();

      Throwable configException = entry.getConfigException();

      if (configException != null)
        throw configException;
    }
    
    clearCache();
  }
  */

  /**
   * Updates an ear deploy
   */
  /*
  public void expandEarDeploy(String name)
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null)
      entry.start();
    
    clearCache();
  }
  */

  /**
   * Start an ear
   */
  /*
  public void startEarDeploy(String name)
  {
    clearCache();
    
    _earDeploy.update();
    EarDeployController entry = _earDeploy.update(name);

    if (entry != null)
      entry.start();
    
    clearCache();
  }
  */

  /**
   * Adds an ear default
   */
  /*
  public void addEarDefault(EarConfig config)
  {
    _earDefaultList.add(config);
  }
  */

  /**
   * Returns the list of ear defaults
   */
  /*
  public ArrayList<EarConfig> getEarDefaultList()
  {
    return _earDefaultList;
  }
  */

  /**
   * Sets the ear-expansion
   */
  /*
  public EarDeployGenerator createEarDeploy()
    throws Exception
  {
    String id = getWebApp().getClusterId() + "/entapp/" + getHost().getIdTail();
    
    return new EarDeployGenerator(id, _earDeploy, getWebApp());
    
    return null;
  }
  */

  /**
   * Adds the ear-expansion
   */
  /*
  public void addEarDeploy(EarDeployGenerator earDeploy)
    throws Exception
  {
    _earDeploy.add(earDeploy);

    // server/26cc - _appDeploy must be added first, because the
    // _earDeploy addition will automaticall register itself
    // XXX: _appDeploy.add(new DeployGeneratorWebAppEar(_appDeploySpi, this, earDeploy));

    _earDeploy.add(earDeploy);
  }
  */

  /**
   * ejb-ref configuration
   */
  public EjbRef createEjbRef()
  {
    WebAppController controller = getController();
    
    if (controller != null && controller.getArchivePath() != null)
      return new EjbRef(controller.getArchivePath());
    else
      return new EjbRef();
  }

  /**
   * ejb-local-ref configuration
   */
  public EjbLocalRef createEjbLocalRef()
  {
    WebAppController controller = getController();
    
    if (controller != null && controller.getArchivePath() != null)
      return new EjbLocalRef(controller.getArchivePath());
    else
      return new EjbLocalRef();
  }

  /*
  public DeployContainer<EnterpriseApplication,EarDeployController> getEarDeployContainer()
  {
    return _earDeploy;
  }
  */
  
  /*
  @Override
  public void setModuleName(String moduleName)
  {
    super.setModuleName(moduleName);
    WebApp webApp = getWebApp();
    
    // XXX: webApp.setModuleName(moduleName);
    
    EjbModule.replace(webApp.getModuleName(), webApp.getClassLoader());
  }
  */

  /**
   * Adds a ResourceRef validator.
   */
  public void addResourceRef(ResourceRef ref)
  {
    // XXX: _resourceValidators.add(ref);
  }

  /**
   * Adds a persistence-context-ref configuration.
   */
  public void addPersistenceContextRef(PersistenceContextRefConfig persistenceContextRefConfig)
    throws ServletException
  {
    // XXX: TCK ejb30/persistence/ee/packaging/web/scope, needs a test case.

    log.fine("WebApp adding persistence context ref: " + persistenceContextRefConfig.getPersistenceContextRefName());

    String unitName = persistenceContextRefConfig.getPersistenceUnitName();

    // log.fine("WebApp looking up entity manager: " + AmberContainer.getPersistenceContextJndiPrefix() + unitName);

    Object obj = null; // Jndi.lookup(AmberContainer.getPersistenceContextJndiPrefix() + unitName);

    log.fine("WebApp found entity manager: " + obj);

    String contextRefName = persistenceContextRefConfig.getPersistenceContextRefName();

    try {
      JndiUtil.bindDeep("java:comp/env/" + contextRefName, obj);
    } catch (NamingException e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * jsp configuration
   */
  @Configurable
  public JspPropertyGroup createJsp()
  {
    if (_jsp == null) {
    }

    return _jsp;
  }

  /**
   * Returns the JSP configuration.
   */
  public JspPropertyGroup getJsp()
  {
    return _jsp;
  }

  /**
   * Returns the JspApplicationContext for EL evaluation.
   */
  public JspApplicationContextImpl getJspApplicationContext()
  {
    return _jspApplicationContext;
  }

  /**
   * Returns a list of the webApps.
   */
  /*
  public EarDeployController []getEntAppList()
  {
    return _earDeploy.getControllers();
  }
  */

  /**
   * taglib configuration
   */
  @Configurable
  public void addTaglib(JspTaglib taglib)
  {
    if (_taglibList == null)
      _taglibList = new ArrayList<JspTaglib>();

    _taglibList.add(taglib);
  }

  /**
   * Returns the taglib configuration.
   */
  public ArrayList<JspTaglib> getTaglibList()
  {
    ArrayList<JspTaglib> taglibs = new ArrayList<JspTaglib>();

    for (int i = 0; _taglibList != null && i < _taglibList.size(); i++) {
      taglibs.add(_taglibList.get(i));
    }

    JspConfig jspConfig = _jspConfig;

    if (jspConfig != null) {
      ArrayList<JspTaglib> taglibList = jspConfig.getTaglibList();
      for (int i = 0; i < taglibList.size(); i++) {
        taglibs.add(taglibList.get(i));
      }
    }

    return taglibs;
  }
  
  @Override
  protected ServletBuilder newServletBuilder()
  {
    return new ServletBuilderResin();
  }

  @Override
  public Collection<TaglibDescriptor> getTaglibs()
  {
    ArrayList<TaglibDescriptor> taglibs
    = new ArrayList<TaglibDescriptor>();

    for (int i = 0; _taglibList != null && i < _taglibList.size(); i++) {
      taglibs.add(_taglibList.get(i));
    }

    JspConfig jspConfig = _jspConfig;

    if (jspConfig != null) {
      ArrayList<JspTaglib> taglibList = jspConfig.getTaglibList();
      for (int i = 0; i < taglibList.size(); i++) {
        taglibs.add(taglibList.get(i));
      }
    }

    return taglibs;
  }

  // @Override
  public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
  {
    JspConfig jspConfig = _jspConfig;

    Collection<JspPropertyGroupDescriptor> propertyGroups
      = new ArrayList<JspPropertyGroupDescriptor>();

    if (jspConfig != null) {
      ArrayList<JspPropertyGroup> groups = jspConfig.getJspPropertyGroupList();
      for (JspPropertyGroup group : groups) {
        propertyGroups.add(group);
      }
    }

    return propertyGroups;
  }

  /**
   * Returns the RoleMapManager
   */
  public RoleMapManager getRoleMapManager()
  {
    return RoleMapManager.create();
  }
  
  public void addRoleMap(XmlRoleMap roleMap)
  {
    getRoleMapManager().addRoleMap(roleMap);
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(RewriteFilter filter)
  {
    if (filter.isRequest()) {
      if (_requestRewriteDispatch == null)
        _requestRewriteDispatch = new RewriteDispatch(getWebApp());

      _requestRewriteDispatch.addAction(filter);
    }
  }

  /**
   * Adds a rewrite dispatch rule
   */
  public void add(DispatchRuleBase rule)
  {
    if (rule.isRequest()) {
      if (_requestRewriteDispatch == null) {
        _requestRewriteDispatch = new RewriteDispatch(getWebApp());
      }

      _requestRewriteDispatch.addRule(rule);
    }
    
    if (rule.isForward()) {
      if (_forwardRewriteDispatch == null) {
        _forwardRewriteDispatch = new RewriteDispatch(getWebApp());
      }

      _forwardRewriteDispatch.addRule(rule);
    }
    
    if (rule.isInclude()) {
      if (_includeRewriteDispatch == null) {
        _includeRewriteDispatch = new RewriteDispatch(getWebApp());
      }

      _includeRewriteDispatch.addRule(rule);
    }
  }

  /**
   * Adds rewrite-dispatch (backwards compat).
   */
  public RewriteDispatch createRewriteDispatch()
  {
    return new RewriteDispatch(getWebApp());
  }

  /**
   * Adds rewrite-dispatch.
   */
  public void addRewriteDispatch(RewriteDispatch dispatch)
  {
    if (dispatch.isRequest())
      _requestRewriteDispatch = dispatch;

    if (dispatch.isInclude())
      _includeRewriteDispatch = dispatch;

    if (dispatch.isForward())
      _forwardRewriteDispatch = dispatch;
  }

  /*
  public RestContainer getRestContainer()
  {
    return _restContainer;
  }

  private void initRest()
  {
    List<Class> resources = new ArrayList<>();
    for (Class resource : _restScanner.getResources()) {
      if (resource.isInterface())
        continue;

      resources.add(resource);
    }

    _restContainer = new RestContainer(getWebApp(),
                                       _restScanner.getApplications(),
                                       resources);

    _restScanner = null;
  }
  */

  /**
   * jsp-config: configuration
   */
  @Configurable
  public JspConfig createJspConfig()
  {
    if (_jspConfig == null) {
      _jspConfig = new JspConfig(getWebApp());
      _jspConfigDescriptor = new JspConfigDescriptorImpl();
    }
    
    return _jspConfig;
  }

  public JspConfig getJspConfig()
  {
    return _jspConfig;
  }

  // @Override
  public JspConfigDescriptor getJspConfigDescriptor()
  {
    return _jspConfigDescriptor;
  }
  
  @Override
  protected WebAppResin createWebApp()
  {
    return new WebAppResin(this);
  }

  @Override
  protected void initConstructor()
  {
    super.initConstructor();
    
    WebAppResinBase webApp = getWebApp();
    
    // the JSP servlet needs to initialize the JspFactory
    JspServlet.initStatic();

    //EjbManager.setScanAll();
    
    PersistenceManager manager = PersistenceManager.create(getClassLoader());

    // called to configure the enhancer when the classloader updates before
    // any loading of the class
    manager.configurePersistenceRoots();
    
    // _restScanner = new RestScanner(getClassLoader(), 3);
    // getClassLoader().addScanListener(_restScanner);

    //EjbModule.replace(webApp.getModuleName(), webApp.getClassLoader());
    //EjbModule.setAppName(webApp.getModuleName(), webApp.getClassLoader());
    //EjbManager.create();
  }
  
  private class JspConfigDescriptorImpl implements JspConfigDescriptor
  {
    @Override
    public Collection<TaglibDescriptor> getTaglibs()
    {
      // return getWebApp().getTaglibs();
      
      return WebAppBuilderResin.this.getTaglibs();
    }

    @Override
    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups()
    {
      //return getWebApp().getJspPropertyGroups();
      return WebAppBuilderResin.this.getJspPropertyGroups();
    }
    
  }
}

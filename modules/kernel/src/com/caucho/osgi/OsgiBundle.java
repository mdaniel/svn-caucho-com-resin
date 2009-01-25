/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.BeanConfig;
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.Loader;
import com.caucho.make.DependencyContainer;
import com.caucho.management.server.OsgiBundleMXBean;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

import org.osgi.framework.*;

/**
 * An osgi-bundle
 */
public class OsgiBundle implements Bundle
{
  private static final L10N L = new L10N(OsgiBundle.class);
  private static final Logger log
    = Logger.getLogger(OsgiBundle.class.getName());

  private final long _id;
  
  private OsgiManager _manager;
  
  private JarPath _jar;
  private Path _path;
  
  private String _symbolicName;
  
  private OsgiVersion _version = new OsgiVersion(0, 0, 0, null);

  private BundleClassLoader _loader;

  private ConfigProgram _program;
  private boolean _isExport;

  private TreeMap<String,String> _headerMap
    = new TreeMap<String,String>();

  private ArrayList<ExportBundleClassLoader> _exportList
    = new ArrayList<ExportBundleClassLoader>();
  
  private ArrayList<PackageItem> _importList
    = new ArrayList<PackageItem>();

  private ArrayList<OsgiServiceRegistration> _serviceList
    = new ArrayList<OsgiServiceRegistration>();

  private ArrayList<ServiceUse> _serviceUseList
    = new ArrayList<ServiceUse>();

  private String _importAttr;

  private String _activatorClassName;
  private OsgiBundleContext _bundleContext;

  private int _state;
  private long _lastModified;

  private OsgiBundleAdmin _admin;

  OsgiBundle(long id,
	     OsgiManager manager,
	     Path path,
	     ConfigProgram program,
	     boolean isExport)
  {
    _id = id;
    _manager = manager;
    _path = path;
    _lastModified = Alarm.getCurrentTime();
    _program = program;
    _isExport = isExport;

    if (log.isLoggable(Level.FINER))
      log.finer(this + " initializing");

    _bundleContext = new OsgiBundleContext(manager, this);

    if (_path != null && _path.getTail().endsWith(".jar"))
      _jar = JarPath.create(_path);

    try {
      if (_jar != null)
	parseManifest(_jar.getManifest());
      else if (_path != null)
	parseManifest(readManifest(_path));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    if (_path != null) {
      _loader = new BundleClassLoader(_manager.getParentLoader(),
				      _symbolicName + "-" + id,
				      _path);
    }
  }

  private void parseManifest(Manifest manifest)
  {
    if (manifest == null)
      throw new ConfigException(L.l("OSGi bundle '{0}' does not have a manifest, which is required by the OSGi specification",
				    _path.getNativePath()));

    Attributes attr = manifest.getMainAttributes();

    for (Map.Entry entry : attr.entrySet()) {
      _headerMap.put(String.valueOf(entry.getKey()),
		     String.valueOf(entry.getValue()));
    }
	
    _symbolicName = attr.getValue("Bundle-SymbolicName");

    if (_symbolicName == null)
      throw new ConfigException(L.l("'{0}' needs a Bundle-SymbolicName in its manifest.  OSGi bundles require a Bundle-SymbolicName",
				    _path.getNativePath()));
	
    String versionString = attr.getValue("Bundle-Version");
	
    _version = OsgiVersion.create(versionString);

    String exportAttr = attr.getValue("Export-Package");

    ArrayList<PackageItem> exportList = null;
      
    if (exportAttr != null)
      exportList = parseItems(exportAttr);

    addExports(exportList);

    _importAttr = attr.getValue("Import-Package");

    if (exportList != null)
      _importList.addAll(exportList);

    if (_importAttr != null) {
      ArrayList<PackageItem> importList = null;

      importList = parseItems(_importAttr);

      _importList.addAll(importList);
    }

    _activatorClassName = attr.getValue("Bundle-Activator");
  }

  private Manifest readManifest(Path path)
    throws IOException
  {
    InputStream is = path.lookup("META-INF/MANIFEST.MF").openRead();

    try {
      return new Manifest(is);
    } finally {
      is.close();
    }
  }

  protected OsgiManager getManager()
  {
    return _manager;
  }
  
  protected ClassLoader getClassLoader()
  {
    return _loader;
  }

  ArrayList<ExportBundleClassLoader> getExports()
  {
    return _exportList;
  }

  public OsgiVersion getVersion()
  {
    return _version;
  }

  void install()
  {
    _state = INSTALLED;

    _admin = new OsgiBundleAdmin(this);
    _admin.register();
  }

  /**
   * Activates the bundle
   */
  void startImpl()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      log.fine(this + " starting");

      InjectManager webBeans = InjectManager.create();

      SingletonBean bean = new SingletonBean(_bundleContext, null,
					       BundleContext.class);

      webBeans.addBean(bean);

      if (_program != null) {
	_program.configure(new BundleConfig());
      }

      _loader.start();
      
      if (_activatorClassName == null) {
	_state = ACTIVE;
      
	return;
      }
      
      log.finer(this + " active with Bundle-Activator=" + _activatorClassName);
    
      Class cl = loadClass(_activatorClassName);

      if (! BundleActivator.class.isAssignableFrom(cl)) {
	throw new ConfigException(L.l("'{0}' does not implement BundleActivator",
				      cl.getName()));
      }

      BundleActivator activator = (BundleActivator) cl.newInstance();

      _state = STARTING;

      activator.start(_bundleContext);
      
      _state = ACTIVE;
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally  {
      thread.setContextClassLoader(oldLoader);
    }
  }

  void resolve()
  {
    _loader.init();

    for (PackageItem item : _importList) {
      String packageName = item.getPrimaryPackage();

      ExportBundleClassLoader loader
	= _manager.getExportLoader(packageName, item.getVersionRange());

      if (loader == null)
	throw new NullPointerException();

      _loader.addImport(packageName, loader);
    }
  }

  private void addExports(ArrayList<PackageItem> exportList)
  {
    if (exportList == null)
      return;

    for (PackageItem item : exportList) {
      String packageName = item.getPrimaryPackage();

      ExportBundleClassLoader loader;

      loader = ExportBundleClassLoader.create(_manager.getParentLoader(),
					      _symbolicName,
					      _version);

      OsgiVersion version = _version;
	
      for (String name : item.getPackageNames()) {
	ExportLoader exportLoader = new ExportLoader(_path, name, version);
	loader.addLoader(exportLoader);

	_manager.putExportLoader(name, loader);

	if (_isExport) {
	  _manager.publishExportLoader(name, loader);
	}
      }
      
      _exportList.add(loader);
    }
  }
  
  private ArrayList<PackageItem> parseItems(String attr)
  {
    ArrayList<PackageItem> items = new ArrayList<PackageItem>();
    PackageItem item = null;

    int i = 0;
    int len = attr.length();

    while (i < len) {
      char ch = attr.charAt(i++);

      if (Character.isWhitespace(ch))
	continue;
      else if (ch == ',') {
	item = null;
	continue;
      }
      else if (ch == ';')
	continue;
      else if (Character.isJavaIdentifierPart(ch)) {
      }
      else {
	throw new ConfigException(L.l("'{0}' is an illegal OSGi package/version string at '{1}'",
				      attr,
				      ch));
      }

      if (item == null) {
	item = new PackageItem();
	items.add(item);
      }

      StringBuilder name = new StringBuilder();

      name.append(ch);

      for (;
	   i < len
	     && (Character.isJavaIdentifierPart((ch = attr.charAt(i)))
		 || ch == '.');
	   i++) {
	name.append(ch);
      }

      for (; i < len && Character.isWhitespace((ch = attr.charAt(i))); i++) {
      }

      if (len <= i || ch != '=') {
	item.addPackage(name.toString());
      }
      else {
	StringBuilder value = new StringBuilder();
	
	for (i++;
	     i < len && Character.isWhitespace((ch = attr.charAt(i)));
	     i++) {
	}

	if (i < len && (ch == '"' || ch == '\'')) {
	  char end = ch;

	  for (i++; i < len && (ch = attr.charAt(i)) != end; i++) {
	    value.append(ch);
	  }

	  i++;
	}
	else {
	  for (;
	       i < len
		 && (ch = attr.charAt(i)) != ',' && ch != ';'
		 && ! Character.isWhitespace(ch);
	       i++) {
	    value.append(ch);
	  }
	}

	item.putAttribute(name.toString(), value.toString());
      }
    }

    return items;
  }

  JarPath getJar()
  {
    return _jar;
  }

  Path getPath()
  {
    return _path;
  }

  Class loadClassImpl(String name)
  {
    for (ExportBundleClassLoader loader : _exportList) {
      try {
	Class cl = loader.loadClassImpl(name, false);

	if (cl != null)
	  return cl;
      } catch (ClassNotFoundException e) {
      }
    }
    
    return null;
  }

  //
  // Bundle API
  //

  /**
   * Returns the bundle's unique id
   */
  public long getBundleId()
  {
    return _id;
  }

  /**
   * Returns the bundle's symbolic name
   */
  public String getSymbolicName()
  {
    return _symbolicName;
  }

  void setSymbolicName(String symbolicName)
  {
    _symbolicName = symbolicName;
  }

  /**
   * Returns the location
   */
  public String getLocation()
  {
    return _path.getURL();
  }

  /**
   * Returns the bundle's current state
   */
  public int getState()
  {
    return _state;
  }

  /**
   * Start the bundle
   */
  public void start(int options)
    throws BundleException
  {
    start();
  }

  /**
   * Start the bundle
   */
  public void start()
    throws BundleException
  {
    _manager.start(this);
  }

  /**
   * Stop the bundle
   */
  public void stop(int options)
    throws BundleException
  {
    stop();
  }

  /**
   * Start the bundle
   */
  public void stop()
    throws BundleException
  {
    throw new IllegalStateException(L.l("Bundle '{0}' can't be stopped because it is a fixed bundle",
					_symbolicName));
  }

  /**
   * Updates the bundle
   */
  public void update()
    throws BundleException
  {
    throw new IllegalStateException(L.l("Bundle '{0}' can't be updated because it is a fixed bundle",
					_symbolicName));
  }

  /**
   * Updates the bundle from an input stream
   */
  public void update(InputStream is)
    throws BundleException
  {
    throw new IllegalStateException(L.l("Bundle '{0}' can't be updated because it is a fixed bundle",
					_symbolicName));
  }

  /**
   * Uninstall the bundle
   */
  public void uninstall()
    throws BundleException
  {
    throw new IllegalStateException(L.l("Bundle '{0}' can't be uninstalled because it is a fixed bundle",
					_symbolicName));
  }

  /**
   * Returns the Manifest headers
   */
  public Dictionary getHeaders()
  {
    return new Hashtable(_headerMap);
  }

  void addService(OsgiServiceRegistration reg)
  {
    synchronized (_serviceList) {
      _serviceList.add(reg);
    }
  }

  void removeService(OsgiServiceRegistration reg)
  {
    synchronized (_serviceList) {
      _serviceList.remove(reg);
    }
  }

  Object getService(ServiceReference ref)
  {
    if (ref == null)
      return null;
    
    synchronized (_serviceUseList) {
      for (int i = 0; i < _serviceUseList.size(); i++) {
	ServiceUse use = _serviceUseList.get(i);

	if (use.getRef() == ref) {
	  use.addUse();
	  return use.getService();
	}
      }
      
      OsgiServiceReference osgiRef = (OsgiServiceReference) ref;

      Object service = osgiRef.getService(this);

      if (service == null)
	return null;

      ServiceFactory factory = null;
      
      if (service instanceof ServiceFactory) {
	factory = (ServiceFactory) service;

	service = factory.getService(this, osgiRef.getRegistration());
      }

      ServiceUse use = new ServiceUse(osgiRef, factory, service);

      _serviceUseList.add(use);

      return service;
    }
  }
  
  boolean ungetService(ServiceReference ref)
  {
    synchronized (_serviceUseList) {
      for (int i = _serviceUseList.size() - 1; i >= 0; i--) {
	ServiceUse use = _serviceUseList.get(i);

	if (use.getRef() == ref) {
	  _serviceUseList.remove(i);

	  if (use.getFactory() != null)
	    use.getFactory().ungetService(this,
					  use.getRef().getRegistration(),
					  use.getService());
	  
	  return true;
	}
      }
    }
      
    return false;
  }

  /**
   * Returns the bundle's registered services
   */
  public ServiceReference []getRegisteredServices()
  {
    synchronized (_serviceList) {
      if (_serviceList.size() == 0)
	return null;
      
      ServiceReference []refs = new ServiceReference[_serviceList.size()];

      for (int i = 0; i < refs.length; i++) {
	refs[i] = _serviceList.get(i).getReference();
      }

      return refs;
    }
  }

  /**
   * Returns the services the bundle is using
   */
  public ServiceReference []getServicesInUse()
  {
    synchronized (_serviceUseList) {
      ServiceReference []refList
	= new ServiceReference[_serviceUseList.size()];
      
      for (int i = 0; i < refList.length; i++) {
	refList[i] = _serviceUseList.get(i).getRef();
      }

      return refList;
    }
  }

  /**
   * Returns true if the bundle has the specified permission
   */
  public boolean hasPermission(Object permission)
  {
    return true;
  }

  /**
   * Returns the localized view of the manifest
   */
  public Dictionary getHeaders(String locale)
  {
    return getHeaders();
  }

  /**
   * Loads a class using the bundle's classloader
   */
  public Class loadClass(String name)
    throws ClassNotFoundException
  {
    return Class.forName(name, false, getClassLoader());
  }

  /**
   * Returns the specified resource from the bundle
   */
  public URL getResource(String name)
  {
    int len = _exportList.size();
    
    for (int i = 0; i < len; i++) {
      ExportBundleClassLoader loader = _exportList.get(i);

      URL url = loader.getResource(name);

      if (url != null)
	return url;
    }
    
    return null;
  }

  /**
   * Returns the resources for the bundle
   */
  public Enumeration getResources(String name)
    throws IOException
  {
    return getClassLoader().getResources(name);
  }

  /**
   * Returns the paths to entries in the bundle
   */
  public Enumeration getEntryPaths(String path)
  {
    URL entry = getEntry(path);

    if (entry != null) {
      Vector vector = new Vector();
      vector.add(entry);
      
      return vector.elements();
    }

    return null;
  }

  /**
   * Returns a URL to the named entry
   */
  public URL getEntry(String pathName)
  {
    Path path = _jar.lookup(pathName);

    try {
      if (path.exists())
	return new URL(path.getURL());
      else
	return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the last modified time of the bundle.
   */
  public long getLastModified()
  {
    return _jar.getContainer().getLastModified();
  }

  /**
   * Returns entries matching a pattern.
   */
  public Enumeration findEntries(String pathName,
				 String filePattern,
				 boolean recurse)
  {
    Path path = _jar.lookup(pathName);

    Vector list = new Vector();
    
    try {
      if (path.exists())
	list.add(new URL(path.getURL()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (list.size() > 0)
      return list.elements();
    else
      return null;
  }

  /**
   * Returns the bundle's context
   */
  public BundleContext getBundleContext()
  {
    return _bundleContext;
  }

  /**
   * Returns the admin
   */
  public OsgiBundleMXBean getAdmin()
  {
    return _admin;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + getBundleId()
	    + "," + getSymbolicName()
	    + "," + getLocation() + "]");
  }

  static class PackageItem {
    private ArrayList<String> _packages = new ArrayList<String>();
    private HashMap<String,String> _attr = new HashMap<String,String>();

    private OsgiVersionRange _versionRange;

    public void addPackage(String packageName)
    {
      _packages.add(packageName);
    }

    public ArrayList<String> getPackageNames()
    {
      return _packages;
    }

    public String getPrimaryPackage()
    {
      if (_packages.size() > 0)
	return _packages.get(0);
      else
	return null;
    }

    public void putAttribute(String key, String value)
    {
      _attr.put(key, value);

      if ("version".equals(key))
	_versionRange = OsgiVersionRange.create(value);
    }

    public OsgiVersion getVersion()
    {
      if (_versionRange != null)
	return _versionRange.getMin();
      else
	return null;
    }

    public OsgiVersionRange getVersionRange()
    {
      return _versionRange;
    }

    public void setVersionRange(OsgiVersionRange version)
    {
      _versionRange = version;
    }

    public String toString()
    {
      if (_versionRange != null) {
	return ("PackageItem[" + _packages
		+ "," + _versionRange
		+ "," + _attr + "]");
      }
      else
	return "PackageItem[" + _packages + "," + _attr + "]";
    }
  }

  static class ServiceUse {
    private final OsgiServiceReference _ref;
    private final ServiceFactory _factory;
    private final Object _service;
    private int _count;

    ServiceUse(OsgiServiceReference ref,
	       ServiceFactory factory,
	       Object service)
    {
      _ref = ref;
      _service = service;
      _factory = factory;
      _count = 1;
    }

    OsgiServiceReference getRef()
    {
      return _ref;
    }

    ServiceFactory getFactory()
    {
      return _factory;
    }

    Object getService()
    {
      return _service;
    }

    void addUse()
    {
      _count++;
    }

    boolean removeUse()
    {
      return --_count > 0;
    }
  }

  /**
   * Configuration for the bundle
   */
  public class BundleConfig implements EnvironmentBean {
    /**
     * Returns the class loader
     */
    public ClassLoader getClassLoader()
    {
      return _loader;
    }

    public ServiceConfig createService()
    {
      return new ServiceConfig();
    }

    /**
     * Adds a namespace bean
     */
    public void addCustomBean(CustomBeanConfig bean)
    {
      ComponentImpl comp = bean.getComponent();

      /*
      if (comp.isService()) {
	WebBeansContainer webBeans
	  = WebBeansContainer.create(_manager.getParentLoader());

	webBeans.addComponent(comp);
      }
      */
    }
  }

  class ServiceConfig extends BeanConfig {
    ServiceConfig()
    {
    }

    public void init()
    {
      super.init();

      if (_comp != null) {
	InjectManager webBeans
	  = InjectManager.create(_manager.getParentLoader());

	webBeans.addBean(_comp);
      }
    }
  }
}

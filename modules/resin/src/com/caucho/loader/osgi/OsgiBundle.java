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

package com.caucho.loader.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.DependencyContainer;
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
  
  private String _symbolicName;
  private String _version = "0.0.0";

  private BundleClassLoader _loader;

  private ArrayList<ExportBundleClassLoader> _exportList
    = new ArrayList<ExportBundleClassLoader>();
  
  private ArrayList<PackageItem> _importList
    = new ArrayList<PackageItem>();

  private String _activatorClassName;
  private OsgiBundleContext _bundleContext;

  private int _state;
  private long _lastModified;

  OsgiBundle(long id, OsgiManager manager, JarPath jar)
  {
    _id = id;
    _manager = manager;
    _jar = jar;
    _lastModified = Alarm.getCurrentTime();

    if (jar != null) {
      try {
	Manifest manifest = jar.getManifest();

	if (manifest == null)
	  throw new ConfigException(L.l("OSGi bundle '{0}' does not have a manifest, which is required by the OSGi specification",
					jar.getContainer().getNativePath()));

	Attributes attr = manifest.getMainAttributes();
	_symbolicName = attr.getValue("Bundle-SymbolicName");

	if (_symbolicName == null)
	  throw new ConfigException(L.l("'{0}' needs a Bundle-SymbolicName in its manifest.  OSGi bundles require a Bundle-SymbolicName",
					jar.getNativePath()));

	String exportAttr = attr.getValue("Export-Package");

	ArrayList<PackageItem> exportList = null;
      
	if (exportAttr != null)
	  exportList = parseItems(exportAttr);

	addExports(exportList);

	if (exportList != null)
	  _importList.addAll(exportList);

	_activatorClassName = attr.getValue("Bundle-Activator");
      } catch (Exception e) {
	throw ConfigException.create(e);
      }

      _loader = new BundleClassLoader(_manager.getParentLoader(),
				      _symbolicName + "-" + id,
				      jar.getContainer());
    }

    _state = INSTALLED;
  }

  OsgiManager getManager()
  {
    return _manager;
  }
  
  ClassLoader getClassLoader()
  {
    return _loader;
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
      
      if (_activatorClassName == null) {
	log.finer(this + " active with no Bundle-Activator");

	_state = ACTIVE;
      
	return;
      }
    
      Class cl = loadClass(_activatorClassName);

      if (! BundleActivator.class.isAssignableFrom(cl)) {
	throw new ConfigException(L.l("'{0}' does not implement BundleActivator",
				      cl.getName()));
      }

      BundleActivator activator = (BundleActivator) cl.newInstance();

      _bundleContext = new OsgiBundleContext(_manager, this);
      
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

      ExportBundleClassLoader loader = _manager.getExportLoader(packageName);

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

      ExportBundleClassLoader loader = _manager.getExportLoader(packageName);

      if (loader == null) {
	loader = ExportBundleClassLoader.create(_manager.getParentLoader(),
						_symbolicName,
						_version);

	String version = _version;
	
	for (String name : item.getPackageNames()) {
	  ExportLoader exportLoader = new ExportLoader(_jar, name, version);
	  loader.addLoader(exportLoader);
	  
	  _manager.putExportLoader(name, loader);
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
      else if (! Character.isJavaIdentifierPart(ch)) {
	throw new ConfigException(L.l("'{0}' is an illegal OSGi package/version string",
				      attr));
      }

      if (item == null) {
	item = new PackageItem();
	items.add(item);
      }

      StringBuilder name = new StringBuilder();

      name.append(ch);

      for (;
	   i < len
	     && Character.isJavaIdentifierPart((ch = attr.charAt(i)));
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

	  for (i++; i < len && ch != end; i++) {
	    value.append(ch);
	  }
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

  /**
   * Returns the location
   */
  public String getLocation()
  {
    return _jar.getURL();
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
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Start the bundle
   */
  public void stop()
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Updates the bundle
   */
  public void update()
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Updates the bundle from an input stream
   */
  public void update(InputStream is)
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Uninstall the bundle
   */
  public void uninstall()
    throws BundleException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the Manifest headers
   */
  public Dictionary getHeaders()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bundle's registered services
   */
  public ServiceReference []getRegisteredServices()
  {
    return new ServiceReference[0];
  }

  /**
   * Returns the services the bundle is using
   */
  public ServiceReference []getServicesInUse()
  {
    return new ServiceReference[0];
  }

  /**
   * Returns true if the bundle has the specified permission
   */
  public boolean hasPermission(Object permission)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the specified resource from the bundle
   */
  public URL getResource(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the localized view of the manifest
   */
  public Dictionary getHeaders(String locale)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Loads a class using the bundle's classloader
   */
  public Class loadClass(String name)
    throws ClassNotFoundException
  {
    return Class.forName(name, false, _loader);
  }

  /**
   * Returns the resources for the bundle
   */
  public Enumeration getResources(String name)
    throws IOException
  {
    return _loader.getResources(name);
  }

  /**
   * Returns the paths to entries in the bundle
   */
  public Enumeration getEntryPaths(String path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a URL to the named entry
   */
  public URL getEntry(String path)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the last modified time of the bundle.
   */
  public long getLastModified()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns entries matching a pattern.
   */
  public Enumeration findEntries(String path,
				 String filePattern,
				 boolean recurse)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bundle's context
   */
  public BundleContext getBundleContext()
  {
    return _bundleContext;
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
    }

    public String toString()
    {
      return "PackageItem[" + _packages + "," + _attr + "]";
    }
  }
}

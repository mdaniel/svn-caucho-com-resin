/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.loader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.loader.enhancer.ScanManager;
import com.caucho.loader.module.ArtifactManager;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.Crc64;
import com.caucho.util.CurrentTime;
import com.caucho.util.LruCache;
import com.caucho.util.ResinThreadPoolExecutor;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
public class EnvironmentClassLoader extends DynamicClassLoader
{
  private static Logger _log;

  private static final Object _childListenerLock = new Object();

  // listeners invoked at the start of any child environment
  private static EnvironmentLocal<ArrayList<EnvironmentListener>> _childListeners;

  // listeners invoked when a Loader is added
  private static EnvironmentLocal<ArrayList<AddLoaderListener>> _addLoaderListeners;

  // The owning bean
  private EnvironmentBean _owner;

  // Class loader specific attributes
  private ConcurrentHashMap<String,Object> _attributes
    = new ConcurrentHashMap<String,Object>(8);

  private ArrayList<ScanListener> _scanListeners;
  private ArrayList<ScanRoot> _pendingScanRoots = new ArrayList<ScanRoot>();

  private AtomicReference<ArtifactManager> _artifactManagerRef
    = new AtomicReference<ArtifactManager>();
  private ArtifactManager _artifactManager;
  
  private ArrayList<String> _packageList = new ArrayList<String>();

  // Array of listeners
  // server/306i  - can't be weak reference, instead create WeakStopListener
  private ArrayList<EnvironmentListener> _listeners
    = new ArrayList<EnvironmentListener>();
  
  private Map<String,String> _resourceAliasMap;
  
  private LruCache<String,ResourceEntry> _resourceCacheMap
    = new LruCache<String,ResourceEntry>(256);

  private WeakStopListener _stopListener;

  // The state of the environment
  private volatile Lifecycle _lifecycle = new Lifecycle();
  private boolean _isConfigComplete;

  private EnvironmentAdmin _admin;

  private Throwable _configException;
  
  //private static AtomicInteger _debugCounter = new AtomicInteger();
  //private int _debugId = _debugCounter.incrementAndGet();

  /**
   * Creates a new environment class loader.
   */
  protected EnvironmentClassLoader(ClassLoader parent, String id)
  {
    this(parent, id, false);
  }

  /**
   * Creates a new environment class loader.
   */
  protected EnvironmentClassLoader(ClassLoader parent, 
                                   String id, 
                                   boolean isRoot)
  {
    super(parent, true, isRoot);
    
    if (id != null)
      setId(id);

    // initializeEnvironment();

    initListeners();
  }

  /**
   * Creates a new environment class loader.
   */
  public static EnvironmentClassLoader create()
  {
    ClassLoader parent = null;
    String id = null;

    return create(parent, id);
  }

  /**
   * Creates a new environment class loader.
   */
  public static EnvironmentClassLoader create(String id)
  {
    ClassLoader parent = null;
    
    return create(parent, id);
  }

  /**
   * Creates a new environment class loader.
   */
  public static EnvironmentClassLoader create(ClassLoader parent)
  {
    String id = null;
    
    return create(parent, id);
  }

  /**
   * Creates a new environment class loader.
   */
  public static EnvironmentClassLoader create(ClassLoader parent, String id)
  {
    if (parent == null)
      parent = Thread.currentThread().getContextClassLoader();
    
    ClassLoader systemClassLoader = getSystemClassLoaderSafe();
    
    if (parent == null || isParent(parent, systemClassLoader))
      parent = systemClassLoader;
    
    String className = System.getProperty("caucho.environment.class.loader");

    if (className != null) {
      try {
        Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(className);
        Constructor<?> ctor = cl.getConstructor(new Class[] { ClassLoader.class, String.class});
        Object instance = ctor.newInstance(parent, id);

        return (EnvironmentClassLoader) instance;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    return new EnvironmentClassLoader(parent, id);
  }
  
  private static boolean isParent(ClassLoader parent, ClassLoader child)
  {
    if (parent == null)
      return true;
    else if (child == null)
      return false;
    else if (parent.equals(child))
      return true;
    else
      return isParent(parent, child.getParent());
  }

  /**
   * Returns the environment's owner.
   */
  public EnvironmentBean getOwner()
  {
    return _owner;
  }

  /**
   * Sets the environment's owner.
   */
  public void setOwner(EnvironmentBean owner)
  {
    _owner = owner;
  }

  /**
   * Sets the config exception.
   */
  public void setConfigException(Throwable e)
  {
    if (_configException == null)
      _configException = e;
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns true if the environment is active
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns the admin
   */
  public EnvironmentMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Initialize the environment.
   */
  @Override
  public void init()
  {
    super.init();

    initEnvironment();
  }

  protected void initEnvironment()
  {
  }

  /**
   * Returns the named attributes
   */
  public Object getAttribute(String name)
  {
    if (_attributes != null)
      return _attributes.get(name);
    else
      return null;
  }

  /**
   * Sets the named attributes
   */
  public Object setAttribute(String name, Object obj)
  {
    if (obj == null) {
      if (_attributes == null)
        return null;
      else
        return _attributes.remove(name);
    }

    if (_attributes == null)
      _attributes = new ConcurrentHashMap<String,Object>(8);

    return _attributes.put(name, obj);
  }

  /**
   * Sets the named attributes
   */
  public Object putIfAbsent(String name, Object obj)
  {
    if (obj == null)
      throw new NullPointerException();

    if (_attributes == null)
      _attributes = new ConcurrentHashMap<String,Object>(8);

    return _attributes.putIfAbsent(name, obj);
  }

  /**
   * Removes the named attributes
   */
  public Object removeAttribute(String name)
  {
    if (_attributes == null)
      return null;
    else
      return _attributes.remove(name);
  }
  
  //
  // resources
  //

  /**
   * Overrides getResource to implement caching.
   */
  @Override
  public URL getResource(String name)
  {
    ResourceEntry entry = _resourceCacheMap.get(name);
    
    if (entry == null || entry.isModified()) {
      URL resource = super.getResource(name);
    
      entry = new ResourceEntry(resource);
    
      _resourceCacheMap.put(name, entry);
    }
    
    return entry.getResource();
  }

  /**
   * Overrides getResource to implement caching.
   */
  @Override
  public InputStream getResourceAsStream(String name)
  {
    ResourceEntry entry = _resourceCacheMap.get(name);
    
    if (entry == null || entry.isModified()) {
      URL resource = super.getResource(name);
    
      entry = new ResourceEntry(resource);
    
      _resourceCacheMap.put(name, entry);
    }
    
    return entry.getResourceAsStream();
  }
  
  //
  // resource aliases
  //
  
  public void putResourceAlias(String name, String actualName)
  {
    if (_resourceAliasMap == null)
      _resourceAliasMap = new ConcurrentHashMap<String,String>();
    
    _resourceAliasMap.put(name, actualName);
  }
  
  @Override
  public String getResourceAlias(String name)
  {
    if (_resourceAliasMap != null) {
      String actualName = _resourceAliasMap.get(name);
      
      if (actualName != null)
        return actualName;
    }
    
    return null;
  }
  
  /**
   * Adds a listener to detect environment lifecycle changes.
   */
  public void addListener(EnvironmentListener listener)
  {
    synchronized (_listeners) {
      for (int i = _listeners.size() - 1; i >= 0; i--) {
        EnvironmentListener oldListener = _listeners.get(i);

        if (listener == oldListener) {
          return;
        }
        else if (oldListener == null)
          _listeners.remove(i);
      }

      _listeners.add(listener);
    }

    if (_lifecycle.isStarting()) {
      listener.environmentBind(this);
    }

    if (_lifecycle.isStarting() && _isConfigComplete) {
      listener.environmentStart(this);
    }
  }

  /**
   * Adds self as a listener.
   */
  private void initListeners()
  {
    ClassLoader parent = getParent();

    for (; parent != null; parent = parent.getParent()) {
      if (parent instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader loader = (EnvironmentClassLoader) parent;

        if (_stopListener == null)
          _stopListener = new WeakStopListener(this);

        loader.addListener(_stopListener);

        return;
      }
    }
  }

  /**
   * Adds a listener to detect environment lifecycle changes.
   */
  public void removeListener(EnvironmentListener listener)
  {
    ArrayList<EnvironmentListener> listeners = _listeners;
    
    if (_listeners == null)
      return;

    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        EnvironmentListener oldListener = listeners.get(i);

        if (listener == oldListener) {
          listeners.remove(i);
          return;
        }
        else if (oldListener == null) {
          listeners.remove(i);
        }
      }
    }
  }

  /**
   * Adds a child listener.
   */
  void addChildListener(EnvironmentListener listener)
  {
    synchronized (_childListenerLock) {
      if (_childListeners == null)
        _childListeners = new EnvironmentLocal<ArrayList<EnvironmentListener>>();

      ArrayList<EnvironmentListener> listeners
        = _childListeners.getLevel(this);

      if (listeners == null) {
        listeners = new ArrayList<EnvironmentListener>();

        _childListeners.set(listeners, this);
      }

      listeners.add(listener);
    }

    if (_lifecycle.isStarting() && _isConfigComplete) {
      listener.environmentStart(this);
    }
  }

  /**
   * Removes a child listener.
   */
  void removeChildListener(EnvironmentListener listener)
  {
    synchronized (_childListenerLock) {
      if (_childListeners == null)
        return;

      ArrayList<EnvironmentListener> listeners
        = _childListeners.getLevel(this);

      if (listeners != null)
        listeners.remove(listener);
    }
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<EnvironmentListener> getEnvironmentListeners()
  {
    ArrayList<EnvironmentListener> listeners;
    listeners = new ArrayList<EnvironmentListener>();

    // add the descendant listeners
    if (_childListeners != null) {
      synchronized (_childListenerLock) {
        ClassLoader loader;

        for (loader = this; loader != null; loader = loader.getParent()) {
          if (loader instanceof EnvironmentClassLoader) {
            ArrayList<EnvironmentListener> childListeners;
            childListeners = _childListeners.getLevel(loader);

            if (childListeners != null)
              listeners.addAll(childListeners);
          }
        }
      }
    }

    if (_listeners == null)
      return listeners;
    
    ArrayList<EnvironmentListener> envListeners = _listeners;

    if (envListeners != null) {
      synchronized (envListeners) {
        for (int i = 0; i < envListeners.size(); i++) {
          EnvironmentListener listener = envListeners.get(i);

          if (listener != null)
            listeners.add(listener);
          else {
            envListeners.remove(i);
            i--;
          }
        }
      }
    }
    
    return listeners;
  }

  /**
   * Adds a child listener.
   */
  public void addLoaderListener(AddLoaderListener listener)
  {
    synchronized (_childListenerLock) {
      if (_addLoaderListeners == null)
        _addLoaderListeners = new EnvironmentLocal<ArrayList<AddLoaderListener>>();

      ArrayList<AddLoaderListener> listeners
        = _addLoaderListeners.getLevel(this);

      if (listeners == null) {
        listeners = new ArrayList<AddLoaderListener>();

        _addLoaderListeners.set(listeners, this);
      }

      if (! listeners.contains(listener)) {
        listeners.add(listener);
      }
    }

    listener.addLoader(this);
  }

  /**
   * Returns the listeners.
   */
  protected ArrayList<AddLoaderListener> getLoaderListeners()
  {
    ArrayList<AddLoaderListener> listeners;
    listeners = new ArrayList<AddLoaderListener>();

    if (_addLoaderListeners == null)
      return listeners;

    // add the descendent listeners
    synchronized (_childListenerLock) {
      ClassLoader loader;

      for (loader = this; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          ArrayList<AddLoaderListener> childListeners;
          childListeners = _addLoaderListeners.getLevel(loader);

          if (childListeners != null)
            listeners.addAll(childListeners);
        }
      }
    }

    return listeners;
  }

  /**
   * Adds a listener to detect class loader changes.
   */
  @Override
  protected void configureEnhancerEvent()
  {
    ArrayList<AddLoaderListener> listeners = getLoaderListeners();

    for (int i = 0;
         listeners != null && i < listeners.size();
         i++) {
      AddLoaderListener listener = listeners.get(i);

      if (listener.isEnhancer())
        listeners.get(i).addLoader(this);
    }
  }

  /**
   * Adds a listener to detect class loader changes.
   */
  @Override
  protected void configurePostEnhancerEvent()
  {
    ArrayList<AddLoaderListener> listeners = getLoaderListeners();

    for (int i = 0;
         listeners != null && i < listeners.size();
         i++) {
      AddLoaderListener listener = listeners.get(i);

      if (! listener.isEnhancer())
        listeners.get(i).addLoader(this);
    }
  }

  /**
   * Adds the URL to the URLClassLoader.
   */
  @Override
  public void addURL(URL url)
  {
    if (containsURL(url))
      return;

    super.addURL(url);

    _pendingScanRoots.add(new ScanRoot(url, null));
  }
  
  /**
   * Adds a virtual module root for scanning.
   * 
   * @param rootPackage
   */
  public void addScanPackage(URL url, String rootPackage)
  {
    if (! containsURL(url)) {
      super.addURL(url);
    }
    
    _packageList.add(rootPackage);

    _pendingScanRoots.add(new ScanRoot(url, rootPackage));
    
    sendAddLoaderEvent();
  }
  
  /**
   * Add the custom packages to the classloader hash.
   */
  @Override
  public String getHash()
  {
    String superHash = super.getHash();
   
    // ioc/0p61 - package needed for hash to enable scan
    long crc = Crc64.generate(superHash);
    
    for (String pkg : _packageList) {
      crc = Crc64.generate(crc, pkg);
    }
    
    return Long.toHexString(crc);
  }

  /**
   * Tells the classloader to scan the root classpath.
   */
  @Override
  public void addScanRoot()
  {
    super.addScanRoot();

    addScanRoot(getParent());
  }
  
  private void addScanRoot(ClassLoader loader)
  {
    if (loader == null)
      return;
    
    addScanRoot(loader.getParent());

    if (loader instanceof URLClassLoader) {
      URLClassLoader urlParent = (URLClassLoader) loader;

      for (URL url : urlParent.getURLs()) {
        // String name = url.toString();
        
        // ejb/0e01
        //if (name.endsWith(".jar")) {
        _pendingScanRoots.add(new ScanRoot(url, null));
        //}
      }
    }
  }

  /**
   * Adds a scan listener.
   */
  public void addScanListener(ScanListener listener)
  {
    if (_scanListeners == null)
      _scanListeners = new ArrayList<ScanListener>();

    int i = 0;
    for (; i < _scanListeners.size(); i++) {
      if (listener.getScanPriority() < _scanListeners.get(i).getScanPriority())
        break;
    }
    _scanListeners.add(i, listener);

    ArrayList<URL> urlList = new ArrayList<URL>();
    for (URL url : getURLs()) {
      if (isScanRootAvailable(url))
        urlList.add(url);
    }

    if (urlList.size() > 0) {
      try {
        make();
      } catch (Exception e) {
        log().log(Level.WARNING, e.toString(), e);

        if (_configException == null)
          _configException = e;
      }

      ArrayList<ScanListener> selfList = new ArrayList<ScanListener>();
      selfList.add(listener);
      ScanManager scanManager = new ScanManager(selfList);

      for (URL url : urlList) {
        scanManager.scan(this, url, null);
      }
    }
  }
  
  private boolean isScanRootAvailable(URL url)
  {
    for (ScanRoot scanRoot : _pendingScanRoots) {
      if (url.equals(scanRoot.getUrl()))
        return false;
    }
    
    return true;
  }

  /**
   * Returns the artifact manager
   */
  public ArtifactManager createArtifactManager()
  {
    if (_artifactManager == null) {
      ArtifactManager manager = new ArtifactManager(this);

      _artifactManagerRef.compareAndSet(null, manager);
      _artifactManager = _artifactManagerRef.get();
    }

    return _artifactManager;
  }

  /**
   * Returns the artifact manager
   */
  public ArtifactManager getArtifactManager()
  {
    return _artifactManager;
  }

  /**
   * Returns any import class, e.g. from an artifact
   */
  @Override
  protected Class<?> findImportClass(String name)
  {
    if (_artifactManager != null)
      return _artifactManager.findImportClass(name);
    else
      return null;
  }

  /**
   * Get resource from an artifact
   */
  @Override
  protected URL getImportResource(String name)
  {
    if (_artifactManager != null)
      return _artifactManager.getImportResource(name);
    else
      return null;
  }

  @Override
  protected void buildImportClassPath(ArrayList<String> cp)
  {
    if (_artifactManager != null)
      _artifactManager.buildImportClassPath(cp);
  }

  /**
   * Applies the action to all visible environment modules.  The
   * action may apply to the same environment more than once.
   */
  public void applyVisibleModules(EnvironmentApply apply)
  {
    apply.apply(this);

    for (ClassLoader parent = getParent();
         parent != null;
         parent = parent.getParent()) {
      if (parent instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader env = (EnvironmentClassLoader) parent;
        env.applyVisibleModules(apply);
        break;
      }
    }

    if (_artifactManager != null)
      _artifactManager.applyVisibleModules(apply);
  }

  /**
   * Called when the <class-loader> completes.
   */
  @Override
  public void validate()
  {
    super.validate();
  }

  @Override
  public void scan()
  {
    configureEnhancerEvent();

    ArrayList<ScanRoot> rootList = new ArrayList<ScanRoot>(_pendingScanRoots);
    _pendingScanRoots.clear();
    
    try {
      int rootListSize = rootList.size();
      
      if (_scanListeners != null && rootListSize > 0) { 
        try {
          make();
        } catch (Exception e) {
          log().log(Level.WARNING, e.toString(), e);

          if (_configException == null)
            _configException = e;
        }

        ScanManager scanManager = new ScanManager(_scanListeners);

        for (int i = 0; i < rootListSize; i++) {
          ScanRoot root = rootList.get(i);

          scanManager.scan(this, root.getUrl(), root.getPackageName());
        }
      }

      // configureEnhancerEvent();
    } catch (Exception e) {
      if (_configException == null)
        _configException = e;
      
      throw ConfigException.create(e);
    }
  }

  /**
   * Starts the config phase of the environment.
   */
  private void config()
  {
    sendAddLoaderEvent();

    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    int size = listeners.size();
    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      if (listener instanceof EnvironmentEnhancerListener) {
        EnvironmentEnhancerListener enhancerListener
          = (EnvironmentEnhancerListener) listener;

        enhancerListener.environmentConfigureEnhancer(this);
      }
    }

    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentConfigure(this);
    }

    // _isConfigComplete = true;
  }

  /**
   * Starts the config phase of the environment.
   */
  private void bind()
  {
    config();

    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    int size = listeners.size();
    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentBind(this);
    }

    _isConfigComplete = true;
  }

  /**
   * Marks the environment of the class loader as started.  The
   * class loader itself doesn't use this, but a callback might.
   */
  public void start()
  {
    if (! _lifecycle.toStarting()) {
      startListeners();
      return;
    }

    sendAddLoaderEvent();

    bind();

    if (_artifactManager != null)
      _artifactManager.start();

    startListeners();

    _admin = new EnvironmentAdmin(this);
    _admin.register();

    _lifecycle.toActive();
  }
  
  private void startListeners()
  {
    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    int size = listeners.size();
    for (int i = 0; listeners != null && i < size; i++) {
      EnvironmentListener listener = listeners.get(i);

      listener.environmentStart(this);
    }
  }

  /**
   * Stops the environment, closing down any resources.
   *
   * The resources are closed in the reverse order that they're started
   */
  @Override
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;
    
    ArrayList<EnvironmentListener> listeners = getEnvironmentListeners();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    thread.setContextClassLoader(this);

    try {
      // closing down in reverse
      if (listeners != null) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
          EnvironmentListener listener = listeners.get(i);

          try {
            listener.environmentStop(this);
          } catch (Throwable e) {
            log().log(Level.WARNING, e.toString(), e);
          }
        }
      }
      
      super.stop();
    } finally {
      thread.setContextClassLoader(oldLoader);

       // drain the thread pool for GC
      ResinThreadPoolExecutor.getThreadPool().stopEnvironment(this);
    }
  }

  /**
   * Destroys the class loader.
   */
  @Override
  public void destroy()
  {
    try {
      WeakStopListener stopListener = _stopListener;
      _stopListener = null;

      super.destroy();

      ClassLoader parent = getParent();
      for (; parent != null; parent = parent.getParent()) {
        if (parent instanceof EnvironmentClassLoader) {
          EnvironmentClassLoader loader = (EnvironmentClassLoader) parent;

          loader.removeListener(stopListener);
        }
      }
    } finally {
      _owner = null;
      _attributes = null;
      _listeners = null;
      _scanListeners = null;
      _artifactManager = null;
      _stopListener = null;

      EnvironmentAdmin admin = _admin;
      _admin = null;

      if (admin != null)
        admin.unregister();
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(getId());

    if (! _lifecycle.isActive()) {
      sb.append(",");
      sb.append(_lifecycle.getStateName());
    }
    sb.append("]");

    return sb.toString();
  }

  private static final Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(EnvironmentClassLoader.class.getName());

    return _log;
  }
  
  private static ClassLoader getSystemClassLoaderSafe()
  {
    try {
      return ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
    }
    
    return EnvironmentClassLoader.class.getClassLoader();
  }
  
  static class ScanRoot {
    private final URL _url;
    private final String _pkg;
    
    ScanRoot(URL url, String pkg)
    {
      _url = url;
      _pkg = pkg;
    }
    
    URL getUrl()
    {
      return _url;
    }
    
    String getPackageName()
    {
      return _pkg;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _url + "," + _pkg + "]";
    }
  }
  
  class ResourceEntry {
    private URL _url;
    private long _expireTime;
    private Path _path;
    private boolean _isPathChecked;
    
    ResourceEntry(URL url)
    {
      _url = url;
      
      if (isDirectoryLoader())
        _expireTime = CurrentTime.getCurrentTime() + getDependencyCheckInterval();
      else
        _expireTime = Long.MAX_VALUE / 2;
    }
    
    public boolean isModified()
    {
      return _expireTime < CurrentTime.getCurrentTime();
    }
    
    public URL getResource()
    {
      return _url;
    }
    
    public InputStream getResourceAsStream()
    {
      if (_url == null)
        return null;

      if (! _isPathChecked) {
        String urlString = URLDecoder.decode(_url.toString());
        
        if (urlString.startsWith("file:") || urlString.startsWith("jar:"))
          _path = Vfs.getPwd(EnvironmentClassLoader.this).lookup(urlString);

        _isPathChecked = true;
      }

      try {
        if (_path != null)
          return _path.openRead();
        else
          return _url.openStream();
      } catch (IOException e) {
        log().log(Level.FINE, e.toString(), e);
        
        return null;
      }
    }
  }
}

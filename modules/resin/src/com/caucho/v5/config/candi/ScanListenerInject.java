/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

package com.caucho.v5.config.candi;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.Environment;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.enhancer.ScanClass;
import com.caucho.v5.loader.enhancer.ScanListener;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Jar;
import com.caucho.v5.vfs.JarPath;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * The web beans container for a given environment.
 */
class ScanListenerInject implements ScanListener {
  private static final Logger log
    = Logger.getLogger(ScanListenerInject.class.getName());
  private static final L10N L = new L10N(ScanListenerInject.class);

  private static String []classpathJars;
  private static String []extPaths;

  private static String javaHome;

  private CandiManager _injectManager;

  private final HashMap<Path, ScanRootInject> _scanRootMap
    = new HashMap<Path, ScanRootInject>();

  private final ArrayList<ScanRootInject> _pendingScanRootList
    = new ArrayList<ScanRootInject>();

  private final ConcurrentHashMap<String, ScanClassInject> _scanClassMap
    = new ConcurrentHashMap<String, ScanClassInject>();

  private final ConcurrentHashMap<NameKey, AnnType> _annotationMap
    = new ConcurrentHashMap<>();
  
  private final ArrayList<ScanClassInject> _immediateResourceList
    = new ArrayList<ScanClassInject>();
  
  private NameKey _nameKey = new NameKey();

  private boolean _isCustomExtension;

  private ArrayList<ScanClassInject> _pendingScanClassList
    = new ArrayList<>();

  ScanListenerInject(CandiManager injectManager)
  {
    _injectManager = injectManager;
  }

  /**
   * Returns the injection manager.
   */
  public CandiManager getInjectManager()
  {
    return _injectManager;
  }

  /**
   * True if a custom extension exists.
   */
  public void setIsCustomExtension(boolean isCustomExtension)
  {
    _isCustomExtension = isCustomExtension;

    if (isCustomExtension) {
      for (ScanClassInject scanClass : _scanClassMap.values()) {
        scanClass.register();
      }
    }
  }

  public boolean isCustomExtension()
  {
    return _isCustomExtension;
  }

  public void scanRoot(ClassLoader loader)
  {
    EnvironmentClassLoader envLoader = Environment.getEnvironmentClassLoader(loader);
    
    ArrayList<String> classPath = new ArrayList<>();
    
    envLoader.buildClassPath(classPath);
    
    for (String pathName : classPath) {
      Path path = Vfs.lookup(pathName);
      
      if (! isBeansPath(path)) {
        continue;
      }

      envLoader.scan(path, this);
    }
  }
  
  private boolean isBeansPath(Path path)
  {
    if (path.getTail().endsWith(".jar")) {
      path = JarPath.create(path);
    }
    
    return path.lookup("META-INF/beans.xml").exists();
  }
  
  public ArrayList<ScanRootInject> getPendingScanRootList()
  {
    ArrayList<ScanRootInject> contextList
      = new ArrayList<>(_pendingScanRootList);

    _pendingScanRootList.clear();

    return contextList;
  }

  public boolean isPending()
  {
    return _pendingScanClassList.size() > 0 || _pendingScanRootList.size() > 0;
  }

  public void addDiscoveredClass(ScanClassInject injectScanClass)
  {
    if (! _pendingScanClassList.contains(injectScanClass)) {
      _pendingScanClassList.add(injectScanClass);
    }
  }

  /**
   * discovers pending beans.
   */
  public void discover()
  {
    //Thread.dumpStack();
    if (_pendingScanClassList.size() == 0) {
      return;
    }
    
    ArrayList<ScanClassInject> pendingScanClassList
      = new ArrayList<>(_pendingScanClassList);

    _pendingScanClassList.clear();

    CandiManager manager = getInjectManager();

    for (ScanClassInject scanClass : pendingScanClassList) {
      if (scanClass.isVetoed()) {
      }
      /*
      else if (isExtension(scanClass)) {
      }
      */
      else {
        manager.discoverBean(scanClass);
      }
    }
  }

  public boolean isExtension(ScanClassInject scanClass)
  {
    if (scanClass.isExtension())
      return true;
    else
      return false;
  }

  Collection<ScanClassInject> getScannedClasses()
  {
    return _scanClassMap.values();
  }

  //
  // ScanListener

  /**
   * Since CDI doesn't enhance, it's priority 1
   */
  @Override
  public int getScanPriority()
  {
    return 1;
  }

  @Override
  public boolean isRootScannable(Path root, String packageRoot)
  {
    if (isExcluded(root) && packageRoot == null) {
      if (log.isLoggable(Level.FINER))
        log.finest("CanDI will not scan " + root.getURL());

      return false;
    }

    ScanRootInject context = _scanRootMap.get(root);

    if (context == null) {
      Path rootManager = root;
      
      if (packageRoot != null) {
        rootManager = root.lookup(packageRoot.replace('.', '/'));
      }
      
      BeanManagerBase beanManager = _injectManager.createBeanManager(rootManager);

      /*
      if (packageRoot != null) {
        _injectManager.getExtensionManager().loadServices(rootManager);
      }
      */
      
      context = new ScanRootInject(root, packageRoot, _injectManager, beanManager);

      _scanRootMap.put(root, context);
      _pendingScanRootList.add(context);
    }
    else if (packageRoot != null) {
      context.update(packageRoot);
    }

    if (context.getDiscoveryMode() == BeanArchive.DiscoveryMode.NONE) {
      context.setScanComplete(true);
    }

    if (context.isScanComplete()) {
      return false;
    }

    context.setScanComplete(true);

    if (log.isLoggable(Level.FINER))
      log.finer("CanDI scanning " + root.getURL());

    return true;
  }

  private boolean isExcluded(Path root)
  {
    if (isJarInClassPath(root)) {
      return true;
    }
    else if (isExtension(root)) {
      return true;
    }
    else if (isJVMJar(root)) {
      return true;
    }
    else {
      return false;
    }
  }

  private boolean isJarInClassPath(Path path)
  {
    if (! (path instanceof JarPath))
      return false;

    String nativePath = ((JarPath) path).getContainer().toString();

    for (String classpathJar : classpathJars) {
      if (nativePath.equals(classpathJar))
        return true;
    }

    return false;
  }

  private boolean isExtension(Path path)
  {
    if (path instanceof JarPath)
      path = ((JarPath) path).getContainer();

    String nativePath = path.getNativePath();

    for (String extPath : extPaths) {
      if (nativePath.startsWith(extPath))
        return true;
    }

    return false;
  }

  private boolean isJVMJar(Path path)
  {
    if (! (path instanceof JarPath))
      return false;

    String jarPath = ((JarPath) path).getContainer().getFullPath();

    if (jarPath.startsWith(javaHome))
      return true;

    return false;
  }

  /**
   * Checks if the class can be a simple class
   */
  @Override
  public ScanClass scanClass(Path root, 
                             String packageRoot,
                             String className,
                             int modifiers)
  {
    // ioc/0j0k - package private allowed

    ScanRootInject context = _scanRootMap.get(root);

    if (context.isClassExcluded(className)) {
      return null;
    }

    if (Modifier.isPrivate(modifiers))
      return null;
    else {
      ScanClassInject scanClass = createScanClass(className, context);

      scanClass.setScanClass();

      return scanClass;
    }
  }

  ScanClassInject getScanClass(String className)
  {
    return _scanClassMap.get(className);
  }

  ScanClassInject createScanClass(String className,
                                  ScanRootInject context)
  {
    ScanClassInject scanClass = _scanClassMap.get(className);

    if (scanClass == null) {
      scanClass = new ScanClassInject(className, this, context);
      
      if (scanClass.isPackageInfo()) {
        context.addPackageInfo(scanClass);
      }
      else {
        ScanClassInject oldScanClass;
        oldScanClass = _scanClassMap.putIfAbsent(className, scanClass);

        if (oldScanClass != null) {
          scanClass = oldScanClass;
        }
      }
    }

    return scanClass;
  }

  /**
   * Loads an annotation for scanning.
   */
  AnnType loadAnnotation(char[] buffer, int offset, int length)
    throws ClassNotFoundException
  {
    NameKey key = _nameKey;
    
    key.init(buffer, offset, length); // new NameKey(buffer, offset, length);

    AnnType annType = _annotationMap.get(key);

    if (annType != null)
      return annType;

    ClassLoader loader = getInjectManager().getClassLoader();

    String className = new String(buffer, offset, length);

    Class<?> cl = Class.forName(className, false, loader);

    annType = new AnnType(cl);

    _annotationMap.put(key.dup(), annType);

    return annType;
  }

  @Override
  public void completePath(Path path)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.finest(L.l("completing scanning of '{0}'", path));
    }
    
    ScanRootInject context = _scanRootMap.get(path);

    if (context.isImplicit()) {
      boolean hasBeans = false;
      
      for (ScanClassInject scanClass : _scanClassMap.values()) {
        if (scanClass.getContext() != context)
          continue;

        hasBeans = scanClass.isRegisterRequired();

        if (hasBeans) {
          break;
        }
      }

      if (! hasBeans) {
        for (Iterator<ScanClassInject> it = _pendingScanClassList.iterator();
             it.hasNext(); ) {
          ScanClassInject scanClass = it.next();

          if (scanClass.getContext() == context) {
            it.remove();
          }
        }

        for (Iterator<ScanClassInject> it = _scanClassMap.values().iterator();
             it.hasNext(); ) {
          ScanClassInject scanClass = it.next();

          if (scanClass.getContext() == context)
            it.remove();
        }
      }
    }

    for (Iterator<ScanClassInject> it = _scanClassMap.values().iterator();
         it.hasNext(); ) {
      ScanClassInject scanClass = it.next();

      if (scanClass.getContext() == context
          && context.isPackageVetoed(scanClass))
        it.remove();
    }

    for (
      Iterator<Map.Entry<String,ScanClassInject>> entries
        = _scanClassMap.entrySet().iterator();
      entries.hasNext(); ) {
      Map.Entry<String,ScanClassInject> entry = entries.next();

      ScanClassInject scanClass = entry.getValue();

      if (scanClass.getContext() != context) {
        continue;
      }

      if (context.getDiscoveryMode() == BeanArchive.DiscoveryMode.ANNOTATED
          && ! scanClass.isRegisterRequired()) {
        entries.remove();
      }
      else {
        scanClass.finishScanOld();
      }
    }
  }

  public void addImmediateResource(ScanClassInject injectScanClass)
  {
    ClassLoader classLoader = _injectManager.getClassLoader();

    if (classLoader instanceof DynamicClassLoader) {
      DynamicClassLoader dynLoader = (DynamicClassLoader) classLoader;
      ClassLoader tmpLoader = dynLoader.getNewTempClassLoader();
      
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      
      try {
        thread.setContextClassLoader(dynLoader);
        
        Class<?> cl = tmpLoader.loadClass(injectScanClass.getClassName());
        
        AnnotatedType<?> annType
          = ReflectionAnnotatedFactory.introspectType(cl);

        for (Annotation ann : cl.getAnnotations()) {
          InjectionPointHandler handler
            = _injectManager.getInjectionPointHandler(ann.annotationType());

          if (handler != null) {
            ConfigProgram program = handler.introspectType(annType);
            
            if (program != null)
              program.inject(null, null);
          }
        }
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        thread.setContextClassLoader(oldLoader);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _injectManager + "]";
  }

  static {
    List<String> classpath = new ArrayList<>();

    String classPath = System.getProperty("java.class.path");

    String []parts = classPath.split(File.pathSeparator);

    for (String part : parts) {
      if (! part.endsWith(".jar") && ! part.endsWith(".zip"))
        continue;

      Path path = Vfs.lookup(part);

      if (path.exists())
        classpath.add(path.getFullPath());
    }

    classpath.toArray(classpathJars = new String[classpath.size()]);

    //
    List<String> extPath = new ArrayList<>();

    String ext = System.getProperty("java.ext.dirs");

    parts = ext.split(File.pathSeparator);

    for (String part : parts) {
      String dir = part.trim();
      if (! dir.isEmpty())
        extPath.add(dir);
    }

    extPath.toArray(extPaths = new String[extPath.size()]);

    javaHome = Vfs.lookup(System.getProperty("java.home")).getFullPath();

    if (javaHome.endsWith("/jre") || javaHome.endsWith("\\jre"))
      javaHome = javaHome.substring(0, javaHome.length() - 4);
  }

  static class NameKey {
    private char[] _buffer;
    private int _offset;
    private int _length;

    NameKey(char[] buffer, int offset, int length)
    {
      init(buffer, offset, length);
    }
    
    NameKey()
    {
    }

    void init(char[] buffer, int offset, int length)
    {
      _buffer = buffer;
      _offset = offset;
      _length = length;
    }

    public NameKey dup()
    {
      char[] buffer = new char[_length];

      System.arraycopy(_buffer, _offset, buffer, 0, _length);

      return new NameKey(buffer, 0, _length);
    }

    public int hashCode()
    {
      char[] buffer = _buffer;
      int offset = _offset;
      int length = _length;
      int hash = length;

      for (length--; length >= 0; length--) {
        char value = buffer[offset + length];

        hash = 65521 * hash + value;
      }

      return hash;
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof NameKey))
        return false;

      NameKey key = (NameKey) o;

      if (_length != key._length)
        return false;

      char[] bufferA = _buffer;
      char[] bufferB = key._buffer;

      int offsetA = _offset;
      int offsetB = key._offset;

      for (int i = _length - 1; i >= 0; i--) {
        if (bufferA[offsetA + i] != bufferB[offsetB + i])
          return false;
      }

      return true;
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "[" + new String(_buffer,
                                                                _offset,
                                                                _length);
    }
  }

  static class AnnType {
    private Class<?> _type;
    private Annotation[] _annotations;

    AnnType(Class<?> type)
    {
      _type = type;
    }

    public Class<?> getType()
    {
      return _type;
    }

    public Annotation[] getAnnotations()
    {
      if (_annotations == null)
        _annotations = _type.getAnnotations();

      return _annotations;
    }
  }
}
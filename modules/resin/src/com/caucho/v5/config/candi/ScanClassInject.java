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

import io.baratine.core.Service;
import io.baratine.core.Startup;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.decorator.Delegate;
import javax.enterprise.context.NormalScope;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.caucho.v5.config.candi.ScanListenerInject.AnnType;
import com.caucho.v5.loader.enhancer.ScanClassBase;

/**
 * The web beans container for a given environment.
 */
public class ScanClassInject extends ScanClassBase
{
  private static final Logger log
    = Logger.getLogger(ScanClassInject.class.getName());
  private static final String PACKAGE_INFO = "package-info";
  
  private static final char []PRODUCES
    = Produces.class.getName().toCharArray();
  private static final char []DISPOSES
    = Disposes.class.getName().toCharArray();
  private static final char []OBSERVES
    = Observes.class.getName().toCharArray();
  private static final char []OBJECT
    = Object.class.getName().toCharArray();
  
  private static final HashSet<Class<?>> _registerAnnotationSet
    = new HashSet<Class<?>>();
  
  private static final HashSet<Class<?>> _immediateResourceSet
    = new HashSet<Class<?>>();
  
  private final String _className;
  private final ScanListenerInject _scanManager;

  private ScanClassInject _parent;
  private ArrayList<ScanClassInject> _children;
  
  private boolean _isScanClass;
  private boolean _isRegisterRequired;
  private boolean _isRegistered;
  
  private boolean _isObserves;
  private boolean _isVetoed;

  final private ScanRootInject _context;
  final private BeanArchive.DiscoveryMode _mode;

  ScanClassInject(String className,
                  ScanListenerInject manager,
                  ScanRootInject context)
  {
    _className = className;
    _scanManager = manager;
    _context = context;
    _mode = context.getDiscoveryMode();
  }

  /**
   * Returns the bean's class name.
   */
  public String getClassName()
  {
    return _className;
  }
  
  public void setScanClass()
  {
    _isScanClass = true;
  }
  
  public boolean isScanClass()
  {
    return _isScanClass;
  }
  
  public BeanManagerBase getBeanManager()
  {
    return _context.getBeanManager();
  }

  /**
   * Returns true if registration is required
   */
  public boolean isRegisterRequired()
  {
    return _isRegisterRequired;
  }
  
  public boolean isRegistered()
  {
    return _isRegistered;
  }
  
  public boolean isObserves()
  {
    return _isObserves;
  }

  public boolean isBeanCandidate()
  {
    return _mode == BeanArchive.DiscoveryMode.ALL;
  }

  @Override
  public void addInterface(char[] buffer, int offset, int length)
  {
    addParent(new String(buffer, offset, length));
  }

  @Override
  public void addSuperClass(char[] buffer, int offset, int length)
  {
    if (isMatch(buffer, offset, length, OBJECT)) {
      return;
    }
    
    addParent(new String(buffer, offset, length));
  }

  @Override
  public void addClassAnnotation(char[] buffer, int offset, int length)
  {
    try {
      AnnType annType = _scanManager.loadAnnotation(buffer, offset, length);
      
      if (annType == null) {
        return;
      }

      if (Vetoed.class == annType.getType()) {
        _isVetoed = true;
        _isRegisterRequired = false;
      }
      
      if (annType.getType() == Observes.class)
        setObserves();
      
      if (_isRegisterRequired) {
        if (_immediateResourceSet.contains(annType.getType())) {
          _scanManager.addImmediateResource(this);
        }
        
        return;
      }
      
      if (_registerAnnotationSet.contains(annType.getType())) {
        _isRegisterRequired = true;
        return;
      }
      
      for (Annotation ann : annType.getAnnotations()) {
        Class<? extends Annotation> metaAnnType = ann.annotationType();
      
        if (metaAnnType == Stereotype.class) {
          _isRegisterRequired = true;
        }
        else if (metaAnnType == Scope.class) {
          _isRegisterRequired = true;
        }
        else if (metaAnnType == NormalScope.class) {
          // ioc/02a3
          _isRegisterRequired = true;
        }
      }
    } catch (ClassNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }
  
  private void setObserves()
  {
    _isObserves = true;
    _isRegisterRequired = true;
    
    // ioc/0b25
    if (_children != null) {
      for (ScanClassInject scanClass : _children) {
        scanClass.setObserves();
      }
    }
  }

  @Override
  public void addPoolString(char[] buffer, int offset, int length)
  {
    if (isMatch(buffer, offset, length, PRODUCES)) {
      _isRegisterRequired = true;
    }
    else if (isMatch(buffer, offset, length, DISPOSES)) {
      _isRegisterRequired = true;
    }
    else if (isMatch(buffer, offset, length, OBSERVES)) {
      setObserves();
    }
  }

  @Override
  public boolean finishScan()
  {
/*
    if (_isRegisterRequired
        || _scanManager.isExtension()
        || _parent != null && _parent.isRegistered()) {
      register();
    }
*/

    return true;
  }

  ScanRootInject getContext()
  {
    return _context;
  }

  public void finishScanOld()
  {
    if (_isRegisterRequired
        || _mode.equals(BeanArchive.DiscoveryMode.ALL)
        || _parent != null && _parent.isRegistered()) {
      register();
    }
  }
  
  private void addParent(String className)
  {
    ScanClassInject parent
      = _scanManager.createScanClass(className, _context);

    parent.addChild(this);
    
    if (_parent == null) // XXX: assert?
      _parent = parent;
  }
  
  private void addChild(ScanClassInject child)
  {
    if (_children == null)
      _children = new ArrayList<>();
    
    if (! _children.contains(child))
      _children.add(child);
    
    if (_isObserves) {
      child.setObserves();
    }
  }
  
  void register()
  {
    if (_isVetoed) {
      return;
    }
    else if (isAnnotation()) {
      return;
    }
    else if (_scanManager.isExtension(this)) {
    }

    if (_isScanClass && ! _isRegistered) {
      _isRegistered = true;

      _scanManager.addDiscoveredClass(this);
    }
    
    if (_children != null) {
      for (ScanClassInject child : _children) {
        child.register();
      }
    }
  }
  
/*
  private boolean isMatch(char []buffer,
                          int offset,
                          int length,
                          char []matchBuffer)
  {
    if (length != matchBuffer.length)
      return false;

    for (int i = 0; i < length; i++) {
      if (buffer[offset + i] != matchBuffer[i])
        return false;
    }

    return true;
  }
*/

  boolean isVetoed()
  {
    return _isVetoed;
  }

  boolean isAnnotation() {
    if (Annotation.class.getName().equals(_className))
      return true;

    if (_parent != null)
      return _parent.isAnnotation();

    return false;
  }

  boolean isExtension()
  {
    if (Extension.class.getName().equals(_className))
      return true;

    if (_parent != null)
      return _parent.isExtension();

    return false;
  }

  boolean isPackageInfo()
  {
    return _className.endsWith(PACKAGE_INFO);
  }

  public String getPackage()
  {
    int packageLength = _className.lastIndexOf('.');

    String packageName;

    if (packageLength > 0) {
      packageName = _className.substring(0, packageLength);
    }
    else {
      packageName = "";
    }

    return packageName;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ScanClassInject that = (ScanClassInject) o;

    if (_className != null ?
      !_className.equals(that._className) :
      that._className != null) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    return _className != null ? _className.hashCode() : 0;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _className + "]";
  }
  
  protected static void addAnnotation(String className)
  {
    try {
      Class<?> cl = Class.forName(className);
      
      _registerAnnotationSet.add(cl);
    } catch (Exception e) {
    }
  }
  
  protected static void addAnnotation(Class<?> cl)
  {
    _registerAnnotationSet.add(cl);
  }
  
  protected static void addImmediate(Class<?> cl)
  {
    _immediateResourceSet.add(cl);
  }

  static {
    // addAnnotation(Inject.class);
    addAnnotation(Named.class);
    addAnnotation(Specializes.class);
    addAnnotation(Delegate.class);
    addAnnotation(Qualifier.class);
    
    addAnnotation(Service.class);
    //addAnnotation(ResourceService.class);    
    //addAnnotation(SessionService.class);
    addAnnotation(Startup.class);
    addAnnotation(Priority.class);
  }
}
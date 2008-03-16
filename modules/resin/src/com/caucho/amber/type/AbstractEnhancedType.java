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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.type;

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.entity.Listener;
import com.caucho.amber.field.StubMethod;
import com.caucho.amber.gen.AmberMappedComponent;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents an abstract enhanced type.
 * Known subclasses: EntityType and ListenerType.
 */
public abstract class AbstractEnhancedType extends Type {
  private static final Logger log = Logger.getLogger(AbstractEnhancedType.class.getName());
  private static final L10N L = new L10N(AbstractEnhancedType.class);

  AmberPersistenceUnit _amberPersistenceUnit;

  JClass _beanClass;
  private String _className;
  private Class _javaBeanClass;

  private String _name;

  private String _instanceClassName;

  private ClassLoader _instanceLoader;
  private Class _instanceClass;

  // XXX: jpa/0u21
  private boolean _isIdClass;

  private boolean _isEnhanced;

  private volatile boolean _isGenerated;

  private Object _instance;

  private Throwable _exception;

  private ArrayList<StubMethod> _methods = new ArrayList<StubMethod>();

  private ArrayList<JMethod> _postLoadCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _prePersistCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postPersistCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _preUpdateCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postUpdateCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _preRemoveCallbacks
    = new ArrayList<JMethod>();

  private ArrayList<JMethod> _postRemoveCallbacks
    = new ArrayList<JMethod>();


  public AbstractEnhancedType(AmberPersistenceUnit amberPersistenceUnit)
  {
    _amberPersistenceUnit = amberPersistenceUnit;
  }

  /**
   * Returns the persistence unit.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return _amberPersistenceUnit;
  }

  public Throwable getConfigException()
  {
    return _exception;
  }

  public void setConfigException(Throwable e)
  {
    if (_exception == null)
      _exception = e;
  }

  /**
   * Sets the bean class.
   */
  public void setBeanClass(JClass beanClass)
  {
    _beanClass = beanClass;
    _className = beanClass.getName();

    if (getName() == null) {
      String name = beanClass.getName();
      int p = name.lastIndexOf('.');

      if (p > 0)
        name = name.substring(p + 1);

      setName(name);
    }
  }

  /**
   * Gets the bean class.
   */
  public JClass getBeanClass()
  {
    return _beanClass;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Returns the java bean class
   */
  public Class getJavaBeanClass()
  {
    if (_javaBeanClass == null) {
      try {
	Thread thread = Thread.currentThread();
	ClassLoader loader = thread.getContextClassLoader();
	
	_javaBeanClass = Class.forName(getClassName(), false, loader);
      } catch (ClassNotFoundException e) {
	throw new AmberRuntimeException(e);
      }
    }
    
    return _javaBeanClass;
  }

  /**
   * Returns the component interface name.
   */
  public String getComponentInterfaceName()
  {
    return null;
  }

  /**
   * Gets a component generator.
   */
  public ClassComponent getComponentGenerator()
  {
    return null;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the instance.
   */
  public void setInstance(Object instance)
  {
    _instance = instance;
  }

  /**
   * Gets the instance.
   */
  public Object getInstance()
  {
    if (_instance == null) {
      try {
        _instance = getInstanceClass().newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return _instance;
  }

  /**
   * Sets the instance class loader
   */
  public void setInstanceClassLoader(ClassLoader loader)
  {
    _instanceLoader = loader;
  }

  public boolean isInit()
  {
    return _instanceClass != null;
  }

  /**
   * Gets the instance class.
   */
  abstract public Class getInstanceClass();

  /**
   * Gets the instance class.
   */
  protected Class getInstanceClass(Class validationInterface)
  {
    if (_instanceClass == null) {
      if (getInstanceClassName() == null) {
        throw new RuntimeException("No instance class:" + this);
      }

      try {
        if (isEnhanced()) {
          ClassLoader loader = getPersistenceUnit().getEnhancedLoader();

          _instanceClass = Class.forName(getBeanClass().getName(), false, loader);
        }
        else {
          ClassLoader loader = _instanceLoader;

          if (loader == null)
            loader = getPersistenceUnit().getEnhancedLoader();

          _instanceClass = Class.forName(getInstanceClassName(), false, loader);
        }
      } catch (ClassNotFoundException e) {
        throw new AmberRuntimeException(e);
      }

      if (! validationInterface.isAssignableFrom(_instanceClass)) {
        if (getConfigException() != null)
          throw new AmberRuntimeException(getConfigException());
        else if (_amberPersistenceUnit.getConfigException() != null)
          throw new AmberRuntimeException(_amberPersistenceUnit.getConfigException());

        throw new AmberRuntimeException(L.l("'{0}' with classloader {1} is an illegal instance class",
                                            _instanceClass.getName(), _instanceClass.getClassLoader()));
      }
    }

    return _instanceClass;
  }

  /**
   * Sets the instance class name.
   */
  public void setInstanceClassName(String className)
  {
    _instanceClassName = className;
  }

  /**
   * Gets the instance class name.
   */
  public String getInstanceClassName()
  {
    return _instanceClassName;
  }

  /**
   * Sets true if the class is enhanced.
   */
  public void setEnhanced(boolean isEnhanced)
  {
    _isEnhanced = isEnhanced;
  }

  /**
   * Returns true if the class is enhanced.
   */
  public boolean isEnhanced()
  {
    return _isEnhanced;
  }

  /**
   * Sets true if the class is a key class, i.e.,
   * some entity is annotated with @IdClass(this.class)
   */
  public void setIdClass(boolean isIdClass)
  {
    // jpa/0u21

    _isIdClass = isIdClass;
  }

  /**
   * Returns true if the class is a key class.
   */
  public boolean isIdClass()
  {
    return _isIdClass;
  }

  /**
   * Returns true if generated.
   */
  public boolean isGenerated()
  {
    return _isGenerated;
  }

  /**
   * Set true if generated.
   */
  public void setGenerated(boolean isGenerated)
  {
    // XXX: ejb/0600 vs ejb/0l00
    if (isEnhanced())
      _isGenerated = isGenerated;
  }

  /**
   * Adds a stub method
   */
  public void addStubMethod(StubMethod method)
  {
    _methods.add(method);
  }

  /**
   * Returns the methods
   */
  public ArrayList<StubMethod> getMethods()
  {
    return _methods;
  }

  /**
   * Adds a @PostLoad callback.
   */
  public void addPostLoadCallback(JMethod callback)
  {
    _postLoadCallbacks.add(callback);
  }

  /**
   * Gets the post-load callback.
   */
  public ArrayList<JMethod> getPostLoadCallbacks()
  {
    return _postLoadCallbacks;
  }

  /**
   * Adds a pre-persist callback.
   */
  public void addPrePersistCallback(JMethod callback)
  {
    _prePersistCallbacks.add(callback);
  }

  /**
   * Gets the pre-persist callback.
   */
  public ArrayList<JMethod> getPrePersistCallbacks()
  {
    return _prePersistCallbacks;
  }

  /**
   * Adds a post-persist callback.
   */
  public void addPostPersistCallback(JMethod callback)
  {
    _postPersistCallbacks.add(callback);
  }

  /**
   * Gets the post-persist callback.
   */
  public ArrayList<JMethod> getPostPersistCallbacks()
  {
    return _postPersistCallbacks;
  }

  /**
   * Adds a pre-update callback.
   */
  public void addPreUpdateCallback(JMethod callback)
  {
    _preUpdateCallbacks.add(callback);
  }

  /**
   * Gets the pre-update callback.
   */
  public ArrayList<JMethod> getPreUpdateCallbacks()
  {
    return _preUpdateCallbacks;
  }

  /**
   * Adds a post-update callback.
   */
  public void addPostUpdateCallback(JMethod callback)
  {
    _postUpdateCallbacks.add(callback);
  }

  /**
   * Gets the post-update callback.
   */
  public ArrayList<JMethod> getPostUpdateCallbacks()
  {
    return _postUpdateCallbacks;
  }

  /**
   * Adds a pre-remove callback.
   */
  public void addPreRemoveCallback(JMethod callback)
  {
    _preRemoveCallbacks.add(callback);
  }

  /**
   * Gets the pre-remove callback.
   */
  public ArrayList<JMethod> getPreRemoveCallbacks()
  {
    return _preRemoveCallbacks;
  }

  /**
   * Adds a post-remove callback.
   */
  public void addPostRemoveCallback(JMethod callback)
  {
    _postRemoveCallbacks.add(callback);
  }

  /**
   * Gets the post-remove callback.
   */
  public ArrayList<JMethod> getPostRemoveCallbacks()
  {
    return _postRemoveCallbacks;
  }

  /**
   * Gets the callbacks.
   */
  public ArrayList<JMethod> getCallbacks(int callbackIndex)
  {
    switch (callbackIndex) {
    case Listener.PRE_PERSIST:
      return _prePersistCallbacks;
    case Listener.POST_PERSIST:
      return _postPersistCallbacks;
    case Listener.PRE_REMOVE:
      return _preRemoveCallbacks;
    case Listener.POST_REMOVE:
      return _postRemoveCallbacks;
    case Listener.PRE_UPDATE:
      return _preUpdateCallbacks;
    case Listener.POST_UPDATE:
      return _postUpdateCallbacks;
    case Listener.POST_LOAD:
      return _postLoadCallbacks;
    }

    return null;
  }

  /**
   * Adds a callback.
   */
  public void addCallback(int callbackIndex,
                          JMethod callback)
  {
    switch (callbackIndex) {
    case Listener.PRE_PERSIST:
      _prePersistCallbacks.add(callback);
      break;
    case Listener.POST_PERSIST:
      _postPersistCallbacks.add(callback);
      break;
    case Listener.PRE_REMOVE:
      _preRemoveCallbacks.add(callback);
      break;
    case Listener.POST_REMOVE:
      _postRemoveCallbacks.add(callback);
      break;
    case Listener.PRE_UPDATE:
      _preUpdateCallbacks.add(callback);
      break;
    case Listener.POST_UPDATE:
      _postUpdateCallbacks.add(callback);
      break;
    case Listener.POST_LOAD:
      _postLoadCallbacks.add(callback);
      break;
    }
  }

  /**
   * Printable version of the listener.
   */
  public String toString()
  {
    return "AbstractEnhancedType[" + _beanClass.getName() + "]";
  }
}

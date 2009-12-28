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

package com.caucho.log;

import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Proxy logger that understands the environment.
 */
class EnvironmentLogger extends Logger implements ClassLoaderListener {
  private static ClassLoader _systemClassLoader;
  
  // The custom local handlers
  private final EnvironmentLocal<Logger> _localLoggers
    = new EnvironmentLocal<Logger>();
  
  // The environment handlers for the Logger
  private final EnvironmentLocal<Handler[]> _localHandlers
    = new EnvironmentLocal<Handler[]>();
  
  // The environment handlers owned by the Logger
  private final EnvironmentLocal<HandlerEntry> _ownHandlers
    = new EnvironmentLocal<HandlerEntry>();

  // The use-parent-handlers value
  private final EnvironmentLocal<Boolean> _useParentHandlers
    = new EnvironmentLocal<Boolean>();

  private boolean _hasLocalLevel;
  
  // Application level override
  private EnvironmentLocal<Level> _localLevel;
  private Level _systemLevel = null;

  private EnvironmentLogger _parent;

  // The finest assigned level accessible from this logger
  private Level _assignedLevel = Level.INFO;
  
  // The finest handler level accessible from this logger
  private Level _handlerLevel = Level.OFF;

  // Can be a weak reference because any configuration in an
  // environment will be held in the EnvironmentLocal.
  private final ArrayList<WeakReference<EnvironmentLogger>> _children
    = new ArrayList<WeakReference<EnvironmentLogger>>();
  
  // Weak list of all the class loaders
  private final ArrayList<WeakReference<ClassLoader>> _loaders
    = new ArrayList<WeakReference<ClassLoader>>();
  
  public EnvironmentLogger(String name, String resourceBundleName)
  {
    super(name, resourceBundleName);
  }

  /**
   * Sets the logger's parent.  This should only be called by the LogManager
   * code.
   */
  public void setParent(Logger parent)
  {
    if (parent.equals(_parent))
      return;
    
    super.setParent(parent);

    if (parent instanceof EnvironmentLogger) {
      _parent = (EnvironmentLogger) parent;

      _assignedLevel = _parent.getAssignedLevel();
      _handlerLevel = _parent.getHandlerLevel();

      setEffectiveLevel();
      
      _parent.addChild(this);
    }
  }

  private Level getHandlerLevel()
  {
    return _handlerLevel;
  }

  /**
   * Adds a new logger as a child, triggered by a setParent.
   */
  void addChild(EnvironmentLogger child)
  {
    _children.add(new WeakReference<EnvironmentLogger>(child));
  }

  /**
   * Adds a handler.
   */
  public synchronized void addHandler(Handler handler)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean hasLoader = false;
    for (int i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);

      if (refLoader == loader)
        hasLoader = true;

      if (isParentLoader(loader, refLoader))
        addHandler(handler, refLoader);
    }

    if (! hasLoader) {
      _loaders.add(new WeakReference<ClassLoader>(loader));
      addHandler(handler, loader);
      Environment.addClassLoaderListener(this, loader);
    }

    HandlerEntry ownHandlers = _ownHandlers.get();
    if (ownHandlers == null) {
      ownHandlers = new HandlerEntry(this);
      _ownHandlers.set(ownHandlers);
    }
    
    ownHandlers.addHandler(handler);
  }

  /**
   * Adds a new handler with a given classloader context.
   */
  private void addHandler(Handler handler, ClassLoader loader)
  {
    // handlers ordered by level
    ArrayList<Handler> handlers = new ArrayList<Handler>();

    handlers.add(handler);

    for (ClassLoader ptr = loader; ptr != null; ptr = ptr.getParent()) {
      Handler []localHandlers = _localHandlers.getLevel(ptr);

      if (localHandlers != null) {
        for (int i = 0; i < localHandlers.length; i++) {
	  int p = handlers.indexOf(localHandlers[i]);

	  if (p < 0) {
	    handlers.add(localHandlers[i]);
	  }
	  else {
	    Handler oldHandler = handlers.get(p);

	    if (localHandlers[i].getLevel().intValue()
		< oldHandler.getLevel().intValue()) {
	      handlers.set(p, localHandlers[i]);
	    }
	  }
        }
      }
    }

    Handler []newHandlers = new Handler[handlers.size()];
    handlers.toArray(newHandlers);
    
    if (loader == _systemClassLoader)
      loader = null;

    _localHandlers.set(newHandlers, loader);

    setHandlerLevel(handler.getLevel());
  }

  /**
   * Removes a handler.
   */
  public synchronized void removeHandler(Handler handler)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (int i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);

      if (isParentLoader(loader, refLoader))
        removeHandler(handler, refLoader);
    }

    HandlerEntry ownHandlers = _ownHandlers.get();
    if (ownHandlers != null)
      ownHandlers.removeHandler(handler);
  }

  private void removeHandler(Handler handler, ClassLoader loader)
  {
    ArrayList<Handler> handlers = new ArrayList<Handler>();

    for (ClassLoader ptr = loader; ptr != null; ptr = ptr.getParent()) {
      Handler []localHandlers = _localHandlers.getLevel(ptr);

      if (localHandlers != null) {
        for (int i = 0; i < localHandlers.length; i++) {
	  if (! localHandlers[i].equals(handler)) {
	    int p = handlers.indexOf(localHandlers[i]);

	    if (p < 0) {
	      handlers.add(localHandlers[i]);
	    }
	    else {
	      Handler oldHandler = handlers.get(p);

	      if (localHandlers[i].getLevel().intValue()
		  < oldHandler.getLevel().intValue()) {
		handlers.set(p, localHandlers[i]);
	      }
	    }
	  }
        }
      }
    }

    Handler []newHandlers = new Handler[handlers.size()];
    handlers.toArray(newHandlers);

    _localHandlers.set(newHandlers, loader);

    setHandlerLevel(handler.getLevel());
  }

  /**
   * Returns true if 'parent' is a parent classloader of 'child'.
   *
   * @param parent the classloader to test as a parent.
   * @param child the classloader to test as a child.
   */
  private boolean isParentLoader(ClassLoader parent, ClassLoader child)
  {
    for (; child != null; child = child.getParent()) {
      if (child == parent)
        return true;
    }

    return false;
  }

  /**
   * Sets a custom logger if possible
   */
  boolean addLogger(Logger logger)
  {
    if (logger.getClass().getName().startsWith("java"))
      return false;
    
    Logger oldLogger = _localLoggers.get();

    if (oldLogger != null)
      return false;

    _localLoggers.set(logger);
    //logger.setParent(_parent);

    return true;
  }

  /**
   * Gets the custom logger if possible
   */
  Logger getLogger()
  {
    return _localLoggers.get();
  }

  /**
   * Logs the message.
   */
  public void log(LogRecord record)
  {
    if (record == null)
      return;
    
    Level level;
    
    if (_localLevel != null) {
      level = _localLevel.get();
   }
    else
      level = _systemLevel;

    if (level != null && record.getLevel().intValue() < level.intValue())
      return;

    for (Logger ptr = this; ptr != null; ptr = ptr.getParent()) {
      Handler handlers[] = ptr.getHandlers();

      if (handlers != null) {
	for (int i = 0; i < handlers.length; i++) {
	  handlers[i].publish(record);
	}
      }

      if (! ptr.getUseParentHandlers())
	break;
    }
  }

  /**
   * Returns the handlers.
   */
  public Handler []getHandlers()
  {
    return _localHandlers.get();
  }

  /**
   * Returns the use-parent-handlers
   */
  public boolean getUseParentHandlers()
  {
    Boolean value = _useParentHandlers.get();

    if (value != null)
      return Boolean.TRUE.equals(value);
    else
      return true;
  }

  /**
   * Sets the use-parent-handlers
   */
  public void setUseParentHandlers(boolean useParentHandlers)
  {
    _useParentHandlers.set(new Boolean(useParentHandlers));
  }

  /**
   * Classloader init callback
   */
  public void classLoaderInit(DynamicClassLoader env)
  {
  }

  /**
   * Classloader destroy callback
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    removeLoader(loader);

    _localHandlers.remove(loader);

    HandlerEntry ownHandlers = _ownHandlers.getLevel(loader);
    if (ownHandlers != null)
      _ownHandlers.remove(loader);

    if (ownHandlers != null)
      ownHandlers.destroy();

    if (_localLevel != null)
      _localLevel.remove(loader);

    updateAssignedLevel();
    updateHandlerLevel();
  }

  /**
   * Application API to set the level.
   *
   * @param level the logging level to set for the logger.
   */
  public void setLevel(Level level)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (loader != null && loader != _systemClassLoader) {
      if (_localLevel == null)
        _localLevel = new EnvironmentLocal<Level>();
      
      _localLevel.set(level);
    }
    else {
      _systemLevel = level;
    }
    
    if (level != null) {
      addLoader(loader);
    }

    updateAssignedLevel();
  }

  /**
   * Adds a class loader to the list of dependency  loaders.
   */
  private void addLoader(ClassLoader loader)
  {
    for (int i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);

      if (refLoader == loader)
	return;
    }

    _loaders.add(new WeakReference<ClassLoader>(loader));
    Environment.addClassLoaderListener(this, loader);
  }

  /**
   * Returns the logger's assigned level.
   */
  public Level getLevel()
  {
    if (_localLevel != null) {
      Level level = _localLevel.get();

      if (level != null) {
	return level;
      }
    }

    return _systemLevel;
  }

  /**
   * Returns the assigned level, calculated through the normal
   * Logger rules, i.e. if unassigned, use the parent's value.
   */
  private Level getAssignedLevel()
  {
    for (Logger log = this; log != null; log = log.getParent()) {
      Level level = log.getLevel();

      if (level != null)
	return level;
    }

    return Level.INFO;
  }

  /**
   * Sets the level, updating any children.
   */
  private void setHandlerLevel(Level level)
  {
    if (_handlerLevel.intValue() <= level.intValue())
      return;

    _handlerLevel = level;
    
    setEffectiveLevel();

    synchronized (this) {
      for (int i = _children.size() - 1; i >= 0; i--) {
        WeakReference<EnvironmentLogger> ref = _children.get(i);
        EnvironmentLogger child = ref.get();

        if (child != null) {
	  // XXX: use parent handlers
          if (_handlerLevel.intValue() < child._handlerLevel.intValue())
            child.setHandlerLevel(level);
        }
        else
          _children.remove(i);
      }
    }
  }

  /**
   * Recalculate the dynamic assigned levels.
   */
  private synchronized void updateAssignedLevel()
  {
    Level oldAssignedLevel = _assignedLevel;
    
    _assignedLevel = Level.INFO;
    
   _hasLocalLevel = false;

    if (_parent != null) {
      _assignedLevel = _parent.getAssignedLevel();
    }
    
    if (_systemLevel != null)
      _assignedLevel = _systemLevel;

    for (int i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader loader = ref.get();

      if (loader == null)
        _loaders.remove(i);

      for (; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

	  updateClassLoaderLevel(envLoader);
        }
      }
      
      updateClassLoaderLevel(ClassLoader.getSystemClassLoader());
    }

    setEffectiveLevel();

    // If this level has become changed permission, need to update all children
    // since they may depend on this value
    if (oldAssignedLevel.intValue() != _assignedLevel.intValue()) {
      for (int i = _children.size() - 1; i >= 0; i--) {
        WeakReference<EnvironmentLogger> ref = _children.get(i);
        EnvironmentLogger child = ref.get();

        if (child != null)
          child.updateAssignedLevel();
        else
          _children.remove(i);
      }
    }
  }

  private void updateClassLoaderLevel(ClassLoader loader)
  {
    if (_localLevel == null) {
      if (_systemLevel != null)
        _assignedLevel = _systemLevel;
      return;
    }
    
    Level localLevel = _localLevel.get(loader);

    if (localLevel != null) {
      if (! _hasLocalLevel)
	_assignedLevel = localLevel;
      else if (localLevel.intValue() < _assignedLevel.intValue())
	_assignedLevel = localLevel;
	    
      _hasLocalLevel = true;
    }
  }

  /**
   * Recalculate the handler level levels.
   */
  private synchronized void updateHandlerLevel()
  {
    Level oldHandlerLevel = _handlerLevel;
    
    _handlerLevel = Level.OFF;

    if (_parent != null)
      _handlerLevel = _parent.getHandlerLevel();

    for (int i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader loader = ref.get();

      if (loader == null)
        _loaders.remove(i);

      for (; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;
	  
          Handler []handlers = _localHandlers.getLevel(envLoader);

          for (int j = 0; handlers != null && j < handlers.length; j++) {
            if (handlers[j].getLevel() != null) {
              Level subLevel = handlers[j].getLevel();

	      if (subLevel.intValue() < _handlerLevel.intValue())
		_handlerLevel = subLevel;
            }
          }
        }
      }
    }

    setEffectiveLevel();

    // If this level has become less permissive, need to update all children
    // since they may depend on this value
    if (oldHandlerLevel.intValue() < _handlerLevel.intValue()) {
      for (int i = _children.size() - 1; i >= 0; i--) {
        WeakReference<EnvironmentLogger> ref = _children.get(i);
        EnvironmentLogger child = ref.get();

        if (child != null)
          child.updateHandlerLevel();
        else
          _children.remove(i);
      }
    }
  }

  /**
   * Sets the static effective logging level.  Use the coarsest level.
   */
  private void setEffectiveLevel()
  {
    if (_handlerLevel.intValue() < _assignedLevel.intValue())
      super.setLevel(_assignedLevel);
    else
      super.setLevel(_handlerLevel);
  }

  /**
   * Removes the specified loader.
   */
  private synchronized void removeLoader(ClassLoader loader)
  {
    int i;
    for (i = _loaders.size() - 1; i >= 0; i--) {
      WeakReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);
      else if (refLoader == loader)
        _loaders.remove(i);
    }
  }

  public String toString()
  {
    return "EnvironmentLogger[" + getName() + "]";
  }

  /**
   * Encapsulates the handler for this logger, keeping a reference in
   * the local environment to avoid GC.
   */
  static class HandlerEntry {
    private final EnvironmentLogger _logger;
    private ArrayList<Handler> _handlers = new ArrayList<Handler>();

    HandlerEntry(EnvironmentLogger logger)
    {
      _logger = logger;
    }

    void addHandler(Handler handler)
    {
      _handlers.add(handler);
    }

    void removeHandler(Handler handler)
    {
      _handlers.remove(handler);
    }

    void destroy()
    {
      ArrayList<Handler> handlers = _handlers;
      _handlers = null;
      
      for (int i = 0; handlers != null && i < handlers.size(); i++) {
	Handler handler = handlers.get(i);

	try {
	  handler.close();
	} catch (Throwable e) {
	  e.printStackTrace();
	}
      }
    }
  }
  
  static {
    try {
      _systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
      // too early to log
    }
  }
}

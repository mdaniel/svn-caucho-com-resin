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

package com.caucho.log;

import java.lang.ref.SoftReference;

import java.util.*;
import java.util.logging.*;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.ClassLoaderListener;

/**
 * Proxy logger that understands the environment.
 */
class EnvironmentLogger extends Logger implements ClassLoaderListener {
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
  private final EnvironmentLocal<Level> _localLevel
    = new EnvironmentLocal<Level>();

  private Logger _parent;
  
  // The lowest level currently configured for this logger.
  private int _level = Level.OFF.intValue();

  // Weak list of all the children
  private final ArrayList<SoftReference<EnvironmentLogger>> _children
    = new ArrayList<SoftReference<EnvironmentLogger>>();
  
  // Weak list of all the class loaders
  private final ArrayList<SoftReference<ClassLoader>> _loaders
    = new ArrayList<SoftReference<ClassLoader>>();
  
  public EnvironmentLogger(String name, String resourceBundleName)
  {
    super(name, resourceBundleName);

    doSetLevel(Level.OFF);
  }

  public void setParent(Logger parent)
  {
    if (parent.equals(_parent))
      return;
    
    super.setParent(parent);

    _parent = parent;

    doSetLevel(parent.getLevel());

    if (parent instanceof EnvironmentLogger) {
      EnvironmentLogger envParent = (EnvironmentLogger) parent;

      envParent.addChild(this);
    }
  }

  void addChild(EnvironmentLogger child)
  {
    _children.add(new SoftReference<EnvironmentLogger>(child));
  }

  /**
   * Adds a handler.
   */
  public synchronized void addHandler(Handler handler)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean hasLoader = false;
    for (int i = _loaders.size() - 1; i >= 0; i--) {
      SoftReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);

      if (refLoader == loader)
        hasLoader = true;

      if (isParentLoader(loader, refLoader))
        addHandler(handler, refLoader);
    }

    if (! hasLoader) {
      _loaders.add(new SoftReference<ClassLoader>(loader));
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

  private void addHandler(Handler handler, ClassLoader loader)
  {
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

    _localHandlers.set(newHandlers);

    if (handler.getLevel().intValue() < _level)
      doSetLevel(handler.getLevel());
  }

  /**
   * Removes a handler.
   */
  public synchronized void removeHandler(Handler handler)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean hasLoader = false;
    for (int i = _loaders.size() - 1; i >= 0; i--) {
      SoftReference<ClassLoader> ref = _loaders.get(i);
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

    _localHandlers.set(newHandlers);

    if (handler.getLevel().intValue() < _level)
      doSetLevel(handler.getLevel());
  }

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

    if (value == null) {
      return super.getUseParentHandlers();
    }
    else
      return Boolean.TRUE.equals(value);
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

    _localLevel.remove(loader);

    updateLevel();
  }

  /**
   * Update the level when class loaders change.
   */
  private synchronized void updateLevel()
  {
    int oldLevel = _level;
    Level level = Level.OFF;

    boolean hasLocalLevel = false;

    if (_parent != null && _parent.getLevel().intValue() < level.intValue()) {
      level = _parent.getLevel();
    }

    for (int i = _loaders.size() - 1; i >= 0; i--) {
      SoftReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader loader = ref.get();

      if (loader == null)
        _loaders.remove(i);

      for (; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

	  if (_localLevel.get(envLoader) != null)
	    hasLocalLevel = true;
	  
          Handler []handlers = _localHandlers.getLevel(envLoader);

          for (int j = 0; handlers != null && j < handlers.length; j++) {
            Level handlerLevel = Level.INFO;
            
            if (handlers[j].getLevel() != null)
              handlerLevel = handlers[j].getLevel();

            if (handlerLevel.intValue() < level.intValue()) {
              level = handlerLevel;
            }
          }
        }
      }
    }

    doSetLevel(level);
    
    _hasLocalLevel = hasLocalLevel;

    // If this level has become less permissive, need to update all children
    // since they may depend on this value
    if (oldLevel < level.intValue()) {
      for (int i = _children.size() - 1; i >= 0; i--) {
        SoftReference<EnvironmentLogger> ref = _children.get(i);
        EnvironmentLogger child = ref.get();

        if (child != null)
          child.updateLevel();
        else
          _children.remove(i);
      }
    }

  }

  /**
   * Application API to set the level.
   */
  public void setLevel(Level level)
  {
    _hasLocalLevel = true;
    
    _localLevel.set(level);

    doSetLevel(level);
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    boolean hasLoader = false;
    for (int i = _loaders.size() - 1; i >= 0; i--) {
      SoftReference<ClassLoader> ref = _loaders.get(i);
      ClassLoader refLoader = ref.get();

      if (refLoader == null)
        _loaders.remove(i);

      if (refLoader == loader)
        return;
    }

    _loaders.add(new SoftReference<ClassLoader>(loader));
    Environment.addClassLoaderListener(this, loader);
  }

  /**
   * Returns the local level.
   */
  public Level getLevel()
  {
    if (_hasLocalLevel) {
      Level level = _localLevel.get();

      if (level != null) {
	return level;
      }
    }

    return super.getLevel();
  }

  /**
   * Returns true for loggable.
   */
  /*
  public boolean isLoggable(Level level)
  {
    int value = level.intValue();
    
    if (value < _level)
      return false;
    else
      return _level < getLevel().intValue();
  }
  */

  /**
   * Sets the level, updating any children.
   */
  public void doSetLevel(Level level)
  {
    super.setLevel(level);

    _level = getLevel().intValue();

    synchronized (this) {
      for (int i = _children.size() - 1; i >= 0; i--) {
        SoftReference<EnvironmentLogger> ref = _children.get(i);
        EnvironmentLogger child = ref.get();

        if (child != null) {
          if (_level < child._level)
            child.doSetLevel(level);
        }
        else
          _children.remove(i);
      }
    }
  }

  /**
   * Removes the specified loader.
   */
  private synchronized void removeLoader(ClassLoader loader)
  {
    int i;
    for (i = _loaders.size() - 1; i >= 0; i--) {
      SoftReference<ClassLoader> ref = _loaders.get(i);
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
}

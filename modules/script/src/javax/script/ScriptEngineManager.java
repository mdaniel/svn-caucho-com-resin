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

package javax.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * A manager for script engines.
 */
public class ScriptEngineManager {
  private static final Logger log =
    Logger.getLogger(ScriptEngineManager.class.getName());

  private ArrayList _engineFactories = new ArrayList();

  protected HashSet engineSpis = new HashSet();
  protected HashMap extensionAssociations = new HashMap();
  protected Namespace globalScope;
  protected HashMap mimeTypeAssociations = new HashMap();
  protected HashMap nameAssociations = new HashMap();

  /**
   * The constructor checks for implementations of the factory.
   */
  public ScriptEngineManager()
  {
    initEngines();
  }

  /**
   * Sets the global scope namespace.
   */
  public void setNamespace(Namespace globalScope)
  {
    this.globalScope = globalScope;
  }

  /**
   * Gets the global scope namespace.
   */
  public Namespace getNamespace()
  {
    return this.globalScope;
  }

  /**
   * Puts a value in the global scope.
   */
  public void put(String key, Object value)
  {
    getNamespace().put(key, value);
  }

  /**
   * Gets a value in the global scope.
   */
  public Object get(String key)
  {
    return getNamespace().get(key);
  }

  /**
   * Returns the engine for the script factory by name.
   */
  public ScriptEngine getEngineByName(String shortName)
  {
    Class fClass = (Class) this.nameAssociations.get(shortName);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the engine for the script factory by extension.
   */
  public ScriptEngine getEngineByExtension(String ext)
  {
    Class fClass = (Class) this.extensionAssociations.get(ext);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the engine for the script factory by mime-type.
   */
  public ScriptEngine getEngineByMimeType(String mimeType)
  {
    Class fClass = (Class) this.mimeTypeAssociations.get(mimeType);

    if (fClass != null)
      return getEngineByClass(fClass);
    else
      return null;
  }

  /**
   * Returns the known factories.
   */
  public ScriptEngineFactory []getEngineFactories()
  {
    ScriptEngineFactory []factories;
    factories = new ScriptEngineFactory[_engineFactories.size()];

    _engineFactories.toArray(factories);

    return factories;
  }

  /**
   * Registers an engine name.
   */
  public void registerEngineName(String name, Class factory)
  {
    this.nameAssociations.put(name, factory);
  }

  /**
   * Registers an engine mime-type.
   */
  public void registerEngineMimeType(String mimeType, Class factory)
  {
    this.mimeTypeAssociations.put(mimeType, factory);
  }

  /**
   * Registers an engine extension
   */
  public void registerEngineExtension(String ext, Class factory)
  {
    this.extensionAssociations.put(ext, factory);
  }

  /**
   * Initialize the script engine.
   */
  private void initEngines()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      Enumeration resources = loader.getResources("META-INF/services/javax.script.ScriptEngineFactory");

      while (resources.hasMoreElements()) {
	URL url = (URL) resources.nextElement();

	InputStream is = url.openStream();
	try {
	  readFactoryFile(is);
	} finally {
	  is.close();
	}
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the script engine.
   */
  private void readFactoryFile(InputStream is)
    throws IOException
  {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;

    while ((line = reader.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
	line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
	addFactoryClass(line);
      }
    }
  }

  /**
   * Handles the factory class
   */
  private void addFactoryClass(String className)
  {
    try {
      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      Class cl = Class.forName(className, false, loader);

      ScriptEngineFactory factory = (ScriptEngineFactory) cl.newInstance();

      if (this.engineSpis.contains(cl))
	return;

      _engineFactories.add(factory);

      this.engineSpis.add(cl);

      String []names = factory.getNames();

      for (int i = 0; i < names.length; i++) {
	registerEngineName(names[i], cl);
      }

      String []mimeTypes = factory.getMimeTypes();

      for (int i = 0; i < mimeTypes.length; i++) {
	registerEngineMimeType(mimeTypes[i], cl);
      }

      String []extensions = factory.getExtensions();

      for (int i = 0; i < extensions.length; i++) {
	registerEngineExtension(extensions[i], cl);
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns an instance of the factory with the given class.
   */
  private ScriptEngine getEngineByClass(Class cl)
  {
    ScriptEngineFactory factory = getFactoryByClass(cl);

    if (factory != null)
      return factory.getScriptEngine();
    else
      return null;
  }

  /**
   * Returns the factory with the given class.
   */
  private ScriptEngineFactory getFactoryByClass(Class cl)
  {
    for (int i = 0; i < _engineFactories.size(); i++) {
      ScriptEngineFactory factory;
      factory = (ScriptEngineFactory) _engineFactories.get(i);

      if (factory.getClass().equals(cl))
	return factory;
    }

    return null;
  }
}


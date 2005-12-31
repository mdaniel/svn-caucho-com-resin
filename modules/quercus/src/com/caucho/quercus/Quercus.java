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

package com.caucho.quercus;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.URL;

import javax.sql.DataSource;

import com.caucho.config.ConfigException;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.VarMap;
import com.caucho.quercus.env.ChainedMap;
import com.caucho.quercus.env.AbstractQuercusClass;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.JavaClassDefinition;
import com.caucho.quercus.env.SessionArrayValue;

import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.module.StaticFunction;

import com.caucho.quercus.page.PhpPage;
import com.caucho.quercus.page.PageManager;

import com.caucho.quercus.parser.PhpParser;

import com.caucho.quercus.program.PhpProgram;
import com.caucho.quercus.program.InterpretedClassDef;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import com.caucho.util.Log;
import com.caucho.util.LruCache;

/**
 * Facade for the PHP language.
 */
public class Quercus {
  private static final Logger log = Log.open(Quercus.class);

  private final PageManager _pageManager;

  private HashMap<String,QuercusModule> _modules
    = new HashMap<String,QuercusModule>();
  
  private HashMap<String,StaticFunction> _staticFunctions
    = new HashMap<String,StaticFunction>();

  private QuercusClass _stdClass;
  
  private HashMap<String,AbstractQuercusClass> _staticClasses
    = new HashMap<String,AbstractQuercusClass>();
  
  private HashMap<String,AbstractQuercusClass> _lowerStaticClasses
    = new HashMap<String,AbstractQuercusClass>();
  
  private HashMap<String,JavaClassDefinition> _javaClassWrappers
    = new HashMap<String,JavaClassDefinition>();
  
  private HashMap<String,JavaClassDefinition> _lowerJavaClassWrappers
    = new HashMap<String,JavaClassDefinition>();

  private HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  private LruCache<String,PhpProgram> _evalCache
    = new LruCache<String,PhpProgram>(256);

  private VarMap<String,Value> _constMap;

  private static HashSet<String> _superGlobals
    = new HashSet<String>();

  // XXX: needs to be a timed LRU
  private HashMap<String,SessionArrayValue> _sessionMap
    = new HashMap<String,SessionArrayValue>();

  private DataSource _database;

  private long _staticId;

  /**
   * Constructor.
   */ 
  public Quercus()
  {
    initStaticFunctions();
    initStaticClasses();
    initStaticClassServices();
    
    _pageManager = new PageManager(this);
  }

  /**
   * Returns the working directory.
   */
  public Path getPwd()
  {
    return _pageManager.getPwd();
  }

  /**
   * Set true if pages should be compiled.
   */
  public void setCompile(boolean isCompile)
  {
    _pageManager.setCompile(isCompile);
  }

  /**
   * Set true if pages should be compiled.
   */
  public void setLazyCompile(boolean isCompile)
  {
    _pageManager.setLazyCompile(isCompile);
  }

  /**
   * Sets the default data source.
   */
  public void setDatabase(DataSource database)
  {
    _database = database;
  }

  /**
   * Gets the default data source.
   */
  public DataSource getDatabase()
  {
    return _database;
  }

  /**
   * Adds a module
   */
  public void addModule(QuercusModule module)
    throws ConfigException
  {
    try {
      introspectPhpModuleClass(module.getClass());
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Adds a java class
   */
  public void addJavaClass(String name, Class type)
    throws ConfigException
  {
    try {
      introspectPhpJavaClass(name, type);
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Adds a java class
   */
  public JavaClassDefinition getJavaClassDefinition(String className)
  {
    JavaClassDefinition def = _javaClassWrappers.get(className);

    if (def != null)
      return def;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class type = Class.forName(className, false, loader);
				 
      def = new JavaClassDefinition(this, className, type);

      _javaClassWrappers.put(className, def);

      def.introspect(this);

      return def;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Finds the java class wrapper.
   */
  public AbstractQuercusClass findJavaClassWrapper(String name)
  {
    AbstractQuercusClass def = _javaClassWrappers.get(name);

    if (def != null)
      return def;

    return _lowerJavaClassWrappers.get(name.toLowerCase());
  }

  /**
   * Sets an ini value.
   */
  public void setIni(String name, StringValue value)
  {
    _iniMap.put(name, value);
  }

  /**
   * Sets an ini value.
   */
  public void setIni(String name, String value)
  {
    setIni(name, new StringValue(value));
  }

  /**
   * Gets an ini value.
   */
  public StringValue getIni(String name)
  {
    return _iniMap.get(name);
  }

  /**
   * Returns the relative path.
   */
  public String getClassName(Path path)
  {
    return _pageManager.getClassName(path);
  }
  
  /**
   * Parses a quercus program.
   *
   * @param path the source file path
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public PhpPage parse(Path path) throws IOException
  {
    return _pageManager.parse(path);
  }
  
  /**
   * Parses a quercus string.
   *
   * @param code the source code
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public PhpProgram parseCode(String code) throws IOException
  {
    PhpProgram program = _evalCache.get(code);

    if (program == null) {
      program = PhpParser.parseEval(this, code);
      _evalCache.put(code, program);
    }

    return program;
  }
  
  /**
   * Parses a quercus string.
   *
   * @param code the source code
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public PhpProgram parseEvalExpr(String code) throws IOException
  {
    // XXX: possible conflict with parse eval because of the
    // return value changes
    PhpProgram program = _evalCache.get(code);

    if (program == null) {
      program = PhpParser.parseEvalExpr(this, code);
      _evalCache.put(code, program);
    }

    return program;
  }
  
  /**
   * Parses a function.
   *
   * @param args the arguments
   * @param code the source code
   *
   * @return the parsed program
   *
   * @throws IOException
   */
  public Value parseFunction(String args, String code) throws IOException
  {
    return PhpParser.parseFunction(this, args, code);
  }

  /**
   * Returns the function with the given name.
   */
  public StaticFunction findFunction(String name)
  {
    return _staticFunctions.get(name);
  }

  /**
   * Returns true if the variable is a superglobal.
   */
  public static boolean isSuperGlobal(String name)
  {
    return _superGlobals.contains(name);
  }

  /**
   * Returns the stdClass definition.
   */
  public QuercusClass getStdClass()
  {
    return _stdClass;
  }
  
  /**
   * Returns the class with the given name.
   */
  public AbstractQuercusClass findClass(String name)
  {
    AbstractQuercusClass def = _staticClasses.get(name);

    if (def == null)
      def = _lowerStaticClasses.get(name.toLowerCase());

    return def;
  }

  /**
   * Returns the class maps.
   */
  public HashMap<String,AbstractQuercusClass> getClassMap()
  {
    return _staticClasses;
  }

  /**
   * Returns the module with the given name.
   */
  public QuercusModule findModule(String name)
  {
    return _modules.get(name);
  }

  /**
   * Returns true if an extension is loaded.
   */
  public boolean isExtensionLoaded(String name)
  {
    for (QuercusModule module : _modules.values()) {
      if (module.isExtensionLoaded(name))
	return true;
    }

    return false;
  }

  /**
   * Returns true if an extension is loaded.
   */
  public Value getExtensionFuncs(String name)
  {
    for (QuercusModule module : _modules.values()) {
      if (module.isExtensionLoaded(name)) {
	// XXX:
	
	return new ArrayValueImpl();
      }
    }

    return BooleanValue.FALSE;
  }

  public VarMap<String,Value> getConstMap()
  {
    return _constMap;
  }

  public String createStaticName()
  {
    return "s" + _staticId++;
  }

  /**
   * Loads the session from the backing.
   */
  public SessionArrayValue loadSession(Env env, String sessionId)
  {
    
    SessionArrayValue session = _sessionMap.get(sessionId);

    if (session != null)
      return (SessionArrayValue) session.copy(env);
    else
      return null;
  }

  /**
   * Saves the session to the backing.
   */
  public void saveSession(Env env, String sessionId, SessionArrayValue session)
  {
    _sessionMap.put(sessionId, (SessionArrayValue) session.copy(env));
  }

  /**
   * Removes the session to the backing.
   */
  public void destroySession(Env env, String sessionId)
  {
    _sessionMap.remove(sessionId);
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusModule
   */ 
  private void initStaticFunctions()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      String quercusModule = "META-INF/services/com.caucho.quercus.QuercusModule";
      Enumeration<URL> urls = loader.getResources(quercusModule);

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        InputStream is = null;
        ReadStream rs =  null;
        try {
          is = url.openStream();
          rs = Vfs.openRead(is);

          parseServicesModule(rs);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
          if (is != null)
            is.close();
        }
      }

    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Parses the services file, looking for PHP services.
   */
  private void parseServicesModule(ReadStream in)
          throws IOException, ClassNotFoundException,
          IllegalAccessException, InstantiationException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String line;

    while ((line = in.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
        String className = line;

	try {
	  Class cl = Class.forName(className, false, loader);

	  introspectPhpModuleClass(cl);
	} catch (Throwable e) {
	  log.info("Failed loading " + className + "\n" + e.toString());
	  log.log(Level.FINE, e.toString(), e);
	}
      }
    }
  }

  /**
   * Introspects the module class for functions.
   *
   * @param cl the class to introspect.
   */
  private void introspectPhpModuleClass(Class cl)
    throws IllegalAccessException, InstantiationException
  {
    log.fine("PHP loading module " + cl.getName());

    QuercusModule module = (QuercusModule) cl.newInstance();

    _modules.put(module.getClass().getName(), module);

    Map<String,Value> map = module.getConstMap();

    if (map != null)
      _constMap = new ChainedMap<String,Value>(_constMap, map);

    for (Field field : cl.getFields()) {
      if (! Modifier.isPublic(field.getModifiers()))
        continue;
      
      if (! Modifier.isStatic(field.getModifiers()))
        continue;
      
      if (! Modifier.isFinal(field.getModifiers()))
        continue;

      Object value = field.get(null);

      if (value == null)
	_constMap.put(field.getName(), NullValue.NULL);
      else if (Byte.class.equals(value.getClass()) ||
	       Short.class.equals(value.getClass()) ||
	       Integer.class.equals(value.getClass()) ||
	       Long.class.equals(value.getClass())) {
	_constMap.put(field.getName(),
		      LongValue.create(((Number) value).longValue()));
      }
      else if (Float.class.equals(value.getClass()) ||
	       Double.class.equals(value.getClass())) {
	_constMap.put(field.getName(),
		      DoubleValue.create(((Number) value).doubleValue()));
      }
      else if (String.class.equals(value.getClass())) {
	_constMap.put(field.getName(),
		      new StringValue((String) value));
      }
      else {
	// XXX: unknown types, e.g. Character?
      }
    }
    
    Map<String,StringValue> iniMap = module.getDefaultIni();

    if (map != null)
      _iniMap.putAll(iniMap);

    for (Method method : cl.getMethods()) {
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      Class retType = method.getReturnType();

      if (void.class.isAssignableFrom(retType))
        continue;

      Class []params = method.getParameterTypes();

      try {
	StaticFunction function = new StaticFunction(this, module, method);

	String methodName = method.getName();

	if (methodName.startsWith("quercus_"))
	  methodName = methodName.substring(8);

	_staticFunctions.put(methodName, function);
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusClass
   */ 
  private void initStaticClassServices()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      String quercusModule = "META-INF/services/com.caucho.quercus.QuercusClass";
      Enumeration<URL> urls = loader.getResources(quercusModule);

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        InputStream is = null;
        ReadStream rs =  null;
        try {
          is = url.openStream();
          rs = Vfs.openRead(is);

          parseClassServicesModule(rs);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
          if (is != null)
            is.close();
        }
      }

    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Parses the services file, looking for PHP services.
   */
  private void parseClassServicesModule(ReadStream in)
          throws IOException, ClassNotFoundException,
          IllegalAccessException, InstantiationException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    String line;

    while ((line = in.readLine()) != null) {
      int p = line.indexOf('#');

      if (p >= 0)
        line = line.substring(0, p);

      line = line.trim();

      if (line.length() > 0) {
        String className = line;

        Class cl = Class.forName(className, false, loader);

	p = className.lastIndexOf('.');

        introspectPhpJavaClass(className.substring(p + 1), cl);
      }
    }
  }

  /**
   * Introspects the module class for functions.
   *
   * @param name the class to introspect.
   */
  private void introspectPhpJavaClass(String name, Class type)
    throws IllegalAccessException, InstantiationException
  {
    JavaClassDefinition def = new JavaClassDefinition(this, name, type);

    _javaClassWrappers.put(name, def);
    _lowerJavaClassWrappers.put(name.toLowerCase(), def);

    _staticClasses.put(name, def);
    _lowerStaticClasses.put(name.toLowerCase(), def);
    
    def.introspect(this);
  }

  /**
   * Scans the classpath for META-INF/services/com.caucho.quercus.QuercusClass
   */ 
  private void initStaticClasses()
  {
    InterpretedClassDef classDef = new InterpretedClassDef("stdClass", null);
    
    _stdClass = new QuercusClass(classDef, null);

    _staticClasses.put(_stdClass.getName(), _stdClass);
    _lowerStaticClasses.put(_stdClass.getName().toLowerCase(), _stdClass);
  }

  public void close()
  {
    _pageManager.close();
  }

  static {
    _superGlobals.add("_SERVER");
    _superGlobals.add("_GET");
    _superGlobals.add("_POST");
    _superGlobals.add("_COOKIE");
    _superGlobals.add("_REQUEST");
    _superGlobals.add("_SESSION");
    _superGlobals.add("GLOBALS");
  }
}


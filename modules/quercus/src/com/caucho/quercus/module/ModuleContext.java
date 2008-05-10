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

package com.caucho.quercus.module;

import com.caucho.config.ConfigException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.function.Marshal;
import com.caucho.quercus.function.MarshalFactory;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class-loader specific context for loaded PHP.
 */
public class ModuleContext
{
  private static L10N L = new L10N(ModuleContext.class);
  private static final Logger log
    = Logger.getLogger(ModuleContext.class.getName());

  private ClassLoader _loader;

  private HashMap<String, QuercusModule> _modules
    = new HashMap<String, QuercusModule>();

  private HashMap<String, ModuleInfo> _moduleInfoMap
    = new HashMap<String, ModuleInfo>();

  private HashSet<String> _extensionSet
    = new HashSet<String>();

  private HashMap<String, Value> _constMap
    = new HashMap<String, Value>();

  private HashMap<String, StaticFunction> _staticFunctions
    = new HashMap<String, StaticFunction>();
  
  private ClassDef _stdClassDef;
  private QuercusClass _stdClass;

  private HashMap<String, ClassDef> _staticClasses
    = new HashMap<String, ClassDef>();

  private HashMap<String, ClassDef> _lowerStaticClasses
    = new HashMap<String, ClassDef>();

  private HashMap<String, JavaClassDef> _javaClassWrappers
    = new HashMap<String, JavaClassDef>();

  private HashMap<String, JavaClassDef> _lowerJavaClassWrappers
    = new HashMap<String, JavaClassDef>();

  private HashMap<String, StringValue> _iniMap
    = new HashMap<String, StringValue>();
  
  private HashMap<String, HashSet<String>> _extensionClasses
    = new HashMap<String, HashSet<String>>();

  protected MarshalFactory _marshalFactory;
  protected ExprFactory _exprFactory;

  /**
   * Constructor.
   */
  public ModuleContext()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Constructor.
   */
  public ModuleContext(ClassLoader loader)
  {
    _loader = loader;
    
    _marshalFactory = new MarshalFactory(this);
    _exprFactory = new ExprFactory();
    
    _stdClassDef = new InterpretedClassDef("stdClass", null, new String[0]);
    _stdClass = new QuercusClass(this, _stdClassDef, null);

    _staticClasses.put(_stdClass.getName(), _stdClassDef);
    _lowerStaticClasses.put(_stdClass.getName().toLowerCase(), _stdClassDef);
  }

  public static ModuleContext getLocalContext(ClassLoader loader)
  {
    throw new UnsupportedOperationException();
    /*
    ModuleContext context = _localModuleContext.getLevel(loader);

    if (context == null) {
      context = new ModuleContext(loader);
      _localModuleContext.set(context, loader);
    }

    return context;
    */
  }

  /**
   * Adds module info.
   */
  public ModuleInfo addModule(String name, QuercusModule module)
    throws ConfigException
  {
    synchronized (this) {
      ModuleInfo info = _moduleInfoMap.get(name);

      if (info == null) {
	info = new ModuleInfo(this, name, module);
	_moduleInfoMap.put(name, info);
      }

      return info;
    }
  }

  public JavaClassDef addClass(String name, Class type,
			       String extension, Class javaClassDefClass)
    throws NoSuchMethodException,
	   InvocationTargetException,
	   IllegalAccessException,
	   InstantiationException
  {
    synchronized (_javaClassWrappers) {
      JavaClassDef def = _javaClassWrappers.get(name);

      if (def == null) {
	if (log.isLoggable(Level.FINEST)) {
	  if (extension == null)
	    log.finest(L.l("PHP loading class {0} with type {1}", name, type.getName()));
	  else
	    log.finest(L.l("PHP loading class {0} with type {1} providing extension {2}", name, type.getName(), extension));
	}

	if (javaClassDefClass != null) {
	  Constructor constructor
	    =  javaClassDefClass.getConstructor(ModuleContext.class,
						String.class,
						Class.class);

	  def = (JavaClassDef) constructor.newInstance(this, name, type);
	}
	else {
	  def = JavaClassDef.create(this, name, type);

	  if (def == null)
	    def = createDefaultJavaClassDef(name, type, extension);
	}

	_javaClassWrappers.put(name, def);
	_lowerJavaClassWrappers.put(name.toLowerCase(), def);

	_staticClasses.put(name, def);
	_lowerStaticClasses.put(name.toLowerCase(), def);

	// def.introspect();

	if (extension != null)
	  _extensionSet.add(extension);
      }

      return def;
    }
  }

  /**
   * Gets or creates a JavaClassDef for the given class name.
   */
  public JavaClassDef getJavaClassDefinition(Class type, String className)
  {
    synchronized (_javaClassWrappers) {
      JavaClassDef def = _javaClassWrappers.get(className);

      if (def != null)
	return def;

      def = JavaClassDef.create(this, className, type);

      if (def == null)
	def = createDefaultJavaClassDef(className, type);

      _javaClassWrappers.put(className, def);

      return def;
    }
  }
  
  /**
   * Adds a java class
   */
  public JavaClassDef getJavaClassDefinition(String className)
  {
    synchronized (_javaClassWrappers) {
      JavaClassDef def = _javaClassWrappers.get(className);

      if (def != null)
	return def;

      try {
	Class type;

	try {
	  type = Class.forName(className, false, _loader);
	}
	catch (ClassNotFoundException e) {
	  throw new ClassNotFoundException(L.l("`{0}' not valid: {1}", className, e.toString()), e);
	}

	def = JavaClassDef.create(this, className, type);

	if (def == null)
	  def = createDefaultJavaClassDef(className, type);

	_javaClassWrappers.put(className, def);

	// def.introspect();

	return def;
      } catch (RuntimeException e) {
	throw e;
      } catch (Exception e) {
	throw new QuercusRuntimeException(e);
      }
    }
  }

  /**
   * Returns a javaClassDef for the given class or null if there is not one.
   */
  public JavaClassDef getJavaClassDefinition(Class javaClass)
  {
    synchronized (_javaClassWrappers) {
      return _javaClassWrappers.get(javaClass.getName());
    }
  }


  protected JavaClassDef createDefaultJavaClassDef(String className,
                                                   Class type)
  {
    return new JavaClassDef(this, className, type);
  }
  
  protected JavaClassDef createDefaultJavaClassDef(String className,
                                                   Class type,
                                                   String extension)
  {
    return new JavaClassDef(this, className, type, extension);
  }
  
  /**
   * Finds the java class wrapper.
   */
  public ClassDef findJavaClassWrapper(String name)
  {
    synchronized (_javaClassWrappers) {
      ClassDef def = _javaClassWrappers.get(name);

      if (def != null)
	return def;
    }

    return _lowerJavaClassWrappers.get(name.toLowerCase());
  }

  public MarshalFactory getMarshalFactory()
  {
    return _marshalFactory;
  }

  public ExprFactory getExprFactory()
  {
    return _exprFactory;
  }
  
  public Marshal createMarshal(Class type,
			       boolean isNotNull,
			       boolean isNullAsFalse)
  {
    return getMarshalFactory().create(type, isNotNull, isNullAsFalse);
  }

  /**
   * Returns an array of the defined functions.
   */
  public ArrayValue getDefinedFunctions()
  {
    ArrayValue internal = new ArrayValueImpl();

    for (String name : _staticFunctions.keySet()) {
      internal.put(name);
    }

    return internal;
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
  public ClassDef findClass(String name)
  {
    ClassDef def = _staticClasses.get(name);

    if (def == null)
      def = _lowerStaticClasses.get(name.toLowerCase());

    return def;
  }

  /**
   * Returns the class maps.
   */
  public HashMap<String, ClassDef> getClassMap()
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
    return _extensionSet.contains(name);
  }

  /**
   * Returns true if an extension is loaded.
   */
  public HashSet<String> getLoadedExtensions()
  {
    return _extensionSet;
  }
  
  /*
   * Adds a class to the extension's list of classes.
   */
  public void addExtensionClass(String ext, String clsName)
  {
    HashSet<String> list = _extensionClasses.get(ext);
    
    if (list == null) {
      list = new HashSet<String>();
      _extensionClasses.put(ext, list);
    }
    
    list.add(clsName);
  }
  
  /*
   * Returns the list of the classes that are part of this extension.
   */
  public HashSet<String> getExtensionClasses(String ext)
  {
    return _extensionClasses.get(ext);
  }

  public HashMap<String, Value> getConstMap()
  {
    return _constMap;
  }

  /**
   * Creates a static function.
   */
  public StaticFunction createStaticFunction(QuercusModule module,
					     Method method)
  {
    return new StaticFunction(this, module, method);
  }

  /**
   * Returns a named constant.
   */
  public Value getConstant(String name)
  {
    return _constMap.get(name);
  }

  public static Value objectToValue(Object obj)
  {
    if (obj == null)
      return NullValue.NULL;
    else if (Byte.class.equals(obj.getClass()) ||
             Short.class.equals(obj.getClass()) ||
             Integer.class.equals(obj.getClass()) ||
             Long.class.equals(obj.getClass())) {
      return LongValue.create(((Number) obj).longValue());
    } else if (Float.class.equals(obj.getClass()) ||
               Double.class.equals(obj.getClass())) {
      return DoubleValue.create(((Number) obj).doubleValue());
    } else if (String.class.equals(obj.getClass())) {
      // XXX: i18n
      return new StringBuilderValue((String) obj);
    } else {
      // XXX: unknown types, e.g. Character?

      return null;
    }
  }
}


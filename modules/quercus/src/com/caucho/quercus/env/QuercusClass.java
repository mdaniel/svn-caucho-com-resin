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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InstanceInitializer;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.TraitAliasMap;
import com.caucho.quercus.program.TraitInsteadofMap;
import com.caucho.util.L10N;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Quercus runtime class.
 */
@SuppressWarnings("serial")
public class QuercusClass extends NullValue {
  private static final L10N L = new L10N(QuercusClass.class);
  private static final Logger log
    = Logger.getLogger(QuercusClass.class.getName());

  private final JavaClassDef _javaClassDef;
  private final ClassDef _classDef;
  private final String _className;

  private QuercusClass _parent;

  private WeakReference<QuercusClass> _cacheRef;

  private boolean _isJavaWrapper;

  private ClassDef []_classDefList;

  private AbstractFunction _constructor;
  private AbstractFunction _destructor;

  private AbstractFunction _fieldGet;
  private AbstractFunction _fieldSet;

  private AbstractFunction _call;
  private AbstractFunction _callStatic;

  private AbstractFunction _invoke;
  private AbstractFunction _toString;
  private AbstractFunction _isset;
  private AbstractFunction _unset;

  private ArrayDelegate _arrayDelegate;
  private TraversableDelegate _traversableDelegate;
  private CountDelegate _countDelegate;

  private final ArrayList<InstanceInitializer> _initializers;

  private final MethodMap<AbstractFunction> _methodMap;
  private final HashMap<StringValue,Expr> _constMap;
  private final HashMap<StringValue,Object> _constJavaMap;
  private final LinkedHashMap<StringValue,ClassField> _fieldMap;
  private final HashMap<String,ArrayList<StaticField>> _staticFieldExprMap;
  private final HashMap<StringValue,StringValue> _staticFieldNameMap;

  private final HashSet<String> _instanceofSet;
  private final HashMap<StringValue,String> _traitMethodBindingClassMap;

  private boolean _isModified;

  private final ModuleContext _moduleContext;

  public QuercusClass(ModuleContext moduleContext,
                      ClassDef classDef,
                      QuercusClass parent)
  {
    _moduleContext = moduleContext;

    _classDef = classDef.loadClassDef(); // force load of any lazy classes
    _className = classDef.getName();
    _parent = parent;

    _initializers = new ArrayList<InstanceInitializer>();

    _fieldMap = new LinkedHashMap<StringValue,ClassField>();
    _methodMap = new MethodMap<AbstractFunction>(this, null);
    _constMap = new HashMap<StringValue,Expr>();
    _constJavaMap = new HashMap<StringValue,Object>();

    _staticFieldExprMap = new LinkedHashMap<String,ArrayList<StaticField>>();

    _staticFieldNameMap = new LinkedHashMap<StringValue,StringValue>();

    if (parent != null) {
      _staticFieldNameMap.putAll(parent._staticFieldNameMap);
    }

    JavaClassDef javaClassDef = null;

    if (classDef instanceof JavaClassDef) {
      javaClassDef = (JavaClassDef) classDef;
      _isJavaWrapper = ! javaClassDef.isDelegate();
    }

    ClassDef []classDefList;

    if (_parent != null) {
      classDefList = new ClassDef[parent._classDefList.length + 1];

      System.arraycopy(parent._classDefList, 0, classDefList, 1,
                       parent._classDefList.length);

      classDefList[0] = classDef;
    }
    else {
      classDefList = new ClassDef[] { classDef };
    }

    _classDefList = classDefList;

    for (int i = 0; i < classDefList.length; i++) {
      if (classDefList[i] instanceof JavaClassDef)
        javaClassDef = (JavaClassDef) classDefList[i];
    }

    _javaClassDef = javaClassDef;

    _instanceofSet = new HashSet<String>();

    HashSet<String> classSet = new HashSet<String>();

    // add interfaces
    for (int i = classDefList.length - 1; i >= 0; i--) {
      classDef = classDefList[i];

      if (classDef == null) {
        throw new NullPointerException("classDef:" + _classDef
                                       + " i:" + i + " parent:" + parent);
      }

      classDef.init();

      addInstances(_instanceofSet, classSet, classDef);
    }

    _traitMethodBindingClassMap = new HashMap<StringValue,String>();

    // then add traits and concrete ancestors
    for (int i = classDefList.length - 1; i >= 1; i--) {
      classDef = classDefList[i];

      classSet.clear();
      addTraitMethods(classSet, classDef);

      classDef.initClassMethods(this, classDef.getName());
      classDef.initClassFields(this, classDef.getName());

      classSet.clear();
      addTraitFields(classSet, classDef);
    }

    // finally add this class
    classSet.clear();
    addTraitMethods(classSet, classDefList[0]);

    classDefList[0].initClassMethods(this, classDefList[0].getName());
    classDefList[0].initClassFields(this, classDefList[0].getName());

    classSet.clear();
    addTraitFields(classSet, classDefList[0]);

    if (_constructor != null && parent != null) {
      if (! _constructor.getName().equals("__construct")
          && ! _className.equals(_constructor.getDeclaringClassName())) {
        // php/093j, php/093n
        addMethodIfNotExist(_moduleContext.createString(_className), _constructor);
      }
    }

    if (_destructor == null && parent != null)
      _destructor = parent.getDestructor();
  }

  private void addInstances(HashSet<String> instanceofSet,
                            HashSet<String> ifaceSet,
                            ClassDef classDef)
  {
    classDef.addInterfaces(instanceofSet);

    // XXX: 2012-12-28 nam: possibly rework this to be more like how it's done
    // for parent classes (e.g. env.addClass(def, classId, parentId, ifaces, traits))

    // _instanceofSet.add(classDef.getName());

    for (String iface : classDef.getInterfaces()) {
      boolean isJavaClassDef = classDef instanceof JavaClassDef;

      QuercusClass cls;

      // XXX: php/0cn2, but this is wrong:
      cls = Env.getInstance().findClass(iface,
                                        ! isJavaClassDef,
                                        true);

      if (cls == null) {
        throw new QuercusRuntimeException(L.l("cannot find interface {0}",
                                              iface));
      }

      // _instanceofSet.addAll(cl.getInstanceofSet());

      ClassDef ifaceDef = cls.getClassDef();
      // ClassDef ifaceDef = moduleContext.findClass(iface);

      if (ifaceDef != null) {
        if (ifaceSet.add(iface)) {
          addInstances(instanceofSet, ifaceSet, ifaceDef);

          ifaceDef.initClassMethods(this, ifaceDef.getName());
          ifaceDef.initClassFields(this, ifaceDef.getName());
        }
      }
    }
  }

  private void addTraitMethods(HashSet<String> traitSet, ClassDef classDef)
  {
    for (String trait : classDef.getTraits()) {
      QuercusClass cls = Env.getInstance().findClass(trait);

      if (cls == null) {
        Env.getInstance().createErrorException(L.l("cannot find trait {0}",
                                                   trait));
      }

      ClassDef traitDef = cls.getClassDef();

      if (traitSet.add(trait)) {
        addTraitMethods(traitSet, traitDef);

        traitDef.initClassMethods(this, classDef.getName());
      }
    }
  }

  private void addTraitFields(HashSet<String> traitSet, ClassDef classDef)
  {
    for (String trait : classDef.getTraits()) {
      QuercusClass cls = Env.getInstance().findClass(trait);

      if (cls == null) {
        Env.getInstance().createErrorException(L.l("cannot find trait {0}",
                                                   trait));
      }

      ClassDef traitDef = cls.getClassDef();

      if (traitSet.add(trait)) {
        addTraitFields(traitSet, traitDef);

        traitDef.initClassFields(this, classDef.getName());
      }
    }
  }

  /**
   * Copy based on a cached value
   */
  public QuercusClass(QuercusClass cacheClass, QuercusClass parent)
  {
    _cacheRef = new WeakReference<QuercusClass>(cacheClass);

    _javaClassDef = cacheClass._javaClassDef;
    _classDef = cacheClass._classDef;
    _className = cacheClass._className;

    _isJavaWrapper = cacheClass._isJavaWrapper;
    _classDefList = cacheClass._classDefList;

    _parent = parent;

    _constructor = cacheClass._constructor;
    _destructor = cacheClass._destructor;

    _fieldGet = cacheClass._fieldGet;
    _fieldSet = cacheClass._fieldSet;

    _call = cacheClass._call;
    _callStatic = cacheClass._callStatic;
    _invoke = cacheClass._invoke;
    _toString = cacheClass._toString;

    _arrayDelegate = cacheClass._arrayDelegate;
    _traversableDelegate = cacheClass._traversableDelegate;
    _countDelegate = cacheClass._countDelegate;

    _initializers = cacheClass._initializers;

    _fieldMap = cacheClass._fieldMap;
    _methodMap = cacheClass._methodMap;
    _constMap = cacheClass._constMap;
    _constJavaMap = cacheClass._constJavaMap;

    _staticFieldExprMap = cacheClass._staticFieldExprMap;
    _staticFieldNameMap = cacheClass._staticFieldNameMap;
    _instanceofSet = cacheClass._instanceofSet;
    _traitMethodBindingClassMap = cacheClass._traitMethodBindingClassMap;

    _moduleContext = cacheClass._moduleContext;
  }

  public ClassDef getClassDef()
  {
    return _classDef;
  }

  public JavaClassDef getJavaClassDef()
  {
    return _javaClassDef;
  }

  public MethodMap<AbstractFunction> getMethodMap()
  {
    return _methodMap;
  }

  public HashSet<String> getInstanceofSet()
  {
    return _instanceofSet;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _className;
  }

  /**
   * Returns the parent class.
   */
  public QuercusClass getParent()
  {
    return _parent;
  }

  /**
   * Returns the module context.
   */
  public ModuleContext getModuleContext()
  {
    return _moduleContext;
  }

  /*
   * Returns the class definitions for this class.
   */
  public ClassDef []getClassDefList()
  {
    return _classDefList;
  }

  /*
   * Returns the name of the extension that this class is part of.
   */
  public String getExtension()
  {
    return _classDef.getExtension();
  }

  public boolean isInterface()
  {
    return _classDef.isInterface();
  }

  public boolean isAbstract()
  {
    return _classDef.isAbstract();
  }

  public boolean isTrait()
  {
    return _classDef.isTrait();
  }

  public boolean isFinal()
  {
    return _classDef.isFinal();
  }

  /**
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }

  /**
   * Gets the constructor.
   */
  public AbstractFunction getConstructor()
  {
    return _constructor;
  }

  /**
   * Sets the destructor.
   */
  public void setDestructor(AbstractFunction fun)
  {
    _destructor = fun;
  }

  /**
   * Gets the destructor.
   */
  public AbstractFunction getDestructor()
  {
    return _destructor;
  }

  /**
   * Returns true if the class is modified for caching.
   */
  public boolean isModified()
  {
    if (_isModified)
      return true;
    else if (_parent != null)
      return _parent.isModified();
    else
      return false;
  }

  /**
   * Mark the class as modified for caching.
   */
  public void setModified()
  {
    if (! _isModified) {
      _isModified = true;

      if (_cacheRef != null) {
        QuercusClass cacheClass = _cacheRef.get();

        if (cacheClass != null)
          cacheClass.setModified();
      }
    }
  }

  /**
   * Sets the array delegate (see ArrayAccess)
   */
  public void setArrayDelegate(ArrayDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} adding array delegate {1}",
                                this,  delegate));

    _arrayDelegate = delegate;
  }

  /**
   * Gets the array delegate (see ArrayAccess)
   */
  public final ArrayDelegate getArrayDelegate()
  {
    return _arrayDelegate;
  }

  /**
   * Sets the traversable delegate
   */
  public void setTraversableDelegate(TraversableDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting traversable delegate {1}",
                                this,  delegate));

    _traversableDelegate = delegate;
  }

  /**
   * Gets the traversable delegate
   */
  public final TraversableDelegate getTraversableDelegate()
  {
    return _traversableDelegate;
  }

  /**
   * Sets the count delegate
   */
  public void setCountDelegate(CountDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting count delegate {1}",
                                this,  delegate));

    _countDelegate = delegate;
  }

  /**
   * Gets the count delegate
   */
  public final CountDelegate getCountDelegate()
  {
    return _countDelegate;
  }

  /**
   * Sets the __fieldGet
   */
  public void setFieldGet(AbstractFunction fun)
  {
    _fieldGet = fun;
  }

  /**
   * Returns the __fieldGet
   */
  public AbstractFunction getFieldGet()
  {
    return _fieldGet;
  }

  /**
   * Sets the __fieldSet
   */
  public void setFieldSet(AbstractFunction fun)
  {
    _fieldSet = fun;
  }

  /**
   * Returns the __fieldSet
   */
  public AbstractFunction getFieldSet()
  {
    return _fieldSet;
  }

  /**
   * Sets the __call
   */
  public void setCall(AbstractFunction fun)
  {
    _call = fun;
  }

  /**
   * Gets the __call
   */
  public AbstractFunction getCall()
  {
    return _call;
  }

  /**
   * Sets the _callStatic
   */
  public void setCallStatic(AbstractFunction fun)
  {
    _callStatic = fun;
  }

  /**
   * Gets the _callStatic
   */
  public AbstractFunction getCallStatic()
  {
    return _callStatic;
  }

  /**
   * Sets the __invoke
   */
  public void setInvoke(AbstractFunction fun)
  {
    _invoke = fun;
  }

  /**
   * Gets the __invoke
   */
  public AbstractFunction getInvoke()
  {
    return _invoke;
  }

  /**
   * Sets the __toString
   */
  public void setToString(AbstractFunction fun)
  {
    _toString = fun;
  }

  /**
   * Gets the __toString
   */
  public AbstractFunction getToString()
  {
    return _toString;
  }

  /**
   * Adds an initializer
   */
  public void addInitializer(InstanceInitializer init)
  {
    _initializers.add(init);
  }

  /**
   * Adds a field.
   */
  public void addField(String declaringClassName,
                       StringValue name,
                       Expr initExpr,
                       FieldVisibility visibility)
  {
    ClassField field = new ClassField(declaringClassName, name,
                                      initExpr, visibility);

    _fieldMap.put(name, field);
  }

  /**
   * Adds a trait field.
   */
  public void addTraitField(String declaringClassName,
                            StringValue name,
                            Expr initExpr,
                            FieldVisibility visibility)
  {
    ClassField existingField = _fieldMap.get(name);

    if (existingField != null
        && ! existingField.getInitValue().equals(initExpr)) {
      Env env = Env.getInstance();
      throw env.createErrorException(L.l("trait field {0}->{1} conflicts with class field {2}->{1}.",
                                         declaringClassName,
                                         name,
                                         getName()));
    }

    boolean isTraitField = true;
    ClassField field = new ClassField(declaringClassName, name,
                                      initExpr, visibility, isTraitField);

    _fieldMap.put(name, field);
  }

  /**
   * Returns a set of the fields and their initial values
   */
  public HashMap<StringValue,ClassField> getClassFields()
  {
    return _fieldMap;
  }

  /**
   * Returns a set of the fields and their initial values
   */
  public ClassField getClassField(StringValue name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Returns a set of the fields and their initial values
   */
  public int findFieldIndex(StringValue name)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the declared functions.
   */
  public Iterable<AbstractFunction> getClassMethods()
  {
    return _methodMap.values();
  }

  /**
   * Adds a method.
   */
  public void addMethod(StringValue name, AbstractFunction fun)
  {
    if (fun == null) {
      throw new NullPointerException(L.l("'{0}' is a null function", name));
    }

    //php/09j9
    // XXX: this is a hack to get Zend Framework running, the better fix is
    // to initialize all interface classes before any concrete classes
    AbstractFunction existingFun = _methodMap.getRaw(name);

    if (existingFun == null || ! fun.isAbstract()) {
      _methodMap.put(name, fun);
    }
    else if (! existingFun.isAbstract() && fun.isAbstract()) {
      Env.getInstance().error(L.l("cannot make non-abstract function {0}:{1}() abstract",
                                  getName(), name));
    }
  }

  /**
   * Adds a method if it does not exist.
   */
  public void addMethodIfNotExist(StringValue name, AbstractFunction fun)
  {
    if (fun == null) {
      throw new NullPointerException(L.l("'{0}' is a null function", name));
    }

    //php/09j9
    // XXX: this is a hack to get Zend Framework running, the better fix is
    // to initialize all interface classes before any concrete classes
    AbstractFunction existingFun = _methodMap.getRaw(name);

    if (existingFun == null && ! fun.isAbstract()) {
      _methodMap.put(name, fun);
    }
  }

  /**
   * Adds a trait method to this class.
   */
  public void addTraitMethod(String bindingClassName,
                             StringValue name,
                             AbstractFunction fun)
  {
    TraitAliasMap aliasMap = _classDef.getTraitAliasMap();

    StringValue alias = null;
    if (aliasMap != null) {
      alias = aliasMap.get(name, fun.getDeclaringClassName());
    }

    addTraitMethod(bindingClassName, name, alias, fun);
  }

  private void addTraitMethod(String bindingClassName,
                              StringValue name,
                              StringValue alias,
                              AbstractFunction fun)
  {
    if (fun == null) {
      throw new NullPointerException(L.l("'{0}' is a null function", name));
    }

    AbstractFunction existingFun = _methodMap.getRaw(name);

    if (existingFun == null || existingFun.isAbstract()) {
      _methodMap.put(name, fun);
      _traitMethodBindingClassMap.put(name, bindingClassName);
    }
    else if (fun.isAbstract()) {
      Env.getInstance().error(L.l("cannot make non-abstract function {0}:{1}() abstract",
                                  getName(), name));
    }
    else if (existingFun.isTraitMethod()) {
      TraitInsteadofMap insteadofMap = _classDef.getTraitInsteadofMap();

      if (insteadofMap == null) {
        traitCollisionError(getName(), name,
                            fun.getDeclaringClassName(),
                            existingFun.getDeclaringClassName());
      }

      int result = insteadofMap.get(name,
                                    fun.getDeclaringClassName(),
                                    existingFun.getDeclaringClassName());

      if (result == TraitInsteadofMap.USE_EXISTING_TRAIT) {
      }
      else if (result == TraitInsteadofMap.USE_NEW_TRAIT) {
        _methodMap.put(name, fun);
        _traitMethodBindingClassMap.put(name, bindingClassName);
      }
      else {
        traitCollisionError(getName(), name,
                            fun.getDeclaringClassName(),
                            existingFun.getDeclaringClassName());
      }
    }
    else {
      _methodMap.put(name, fun);
      _traitMethodBindingClassMap.put(name, bindingClassName);
    }

    if (alias != null) {
      addTraitMethod(bindingClassName, alias, null, fun);
    }
  }

  private static final void traitCollisionError(String className,
                                                StringValue funName,
                                                String traitName,
                                                String existingTraitName)
  {
    Env.getInstance().error(L.l("cannot add trait method {0}::{1}() to class {2}"
                                + " because it collides with trait method {3}::{1}() ",
                                traitName, funName,
                                className, existingTraitName));
  }

  /**
   * Adds a static class field.
   */
  public void addStaticFieldExpr(String className, StringValue name, Expr value)
  {
    ArrayList<StaticField> fieldList = _staticFieldExprMap.get(className);

    if (fieldList == null) {
      fieldList = new ArrayList<StaticField>();

      _staticFieldExprMap.put(className, fieldList);
    }

    fieldList.add(new StaticField(name, value));

    StringValue sb = createStringBuilder();
    sb.append(className);
    sb.append("::");
    sb.append(name);

    _staticFieldNameMap.put(name, sb);
  }

  /**
   * Adds a static class field.
   */
  public void addStaticTraitFieldExpr(String bindingClassName,
                                      StringValue name,
                                      Expr value)
  {
    ArrayList<StaticField> fieldList = _staticFieldExprMap.get(bindingClassName);

    if (fieldList == null) {
      fieldList = new ArrayList<StaticField>();

      _staticFieldExprMap.put(bindingClassName, fieldList);
    }

    fieldList.add(new StaticField(name, value));

    StringValue sb = createStringBuilder();
    sb.append(bindingClassName);
    sb.append("::");
    sb.append(name);

    _staticFieldNameMap.put(name, sb);
  }

  private StringValue createString(String s)
  {
    if (_moduleContext.isUnicodeSemantics()) {
      return new UnicodeBuilderValue(s);
    }
    else {
      return new ConstStringValue(s);
    }
  }

  private StringValue createStringBuilder()
  {
    if (_moduleContext.isUnicodeSemantics()) {
      return new UnicodeBuilderValue();
    }
    else {
      return new StringBuilderValue();
    }
  }

  /**
   * Returns the static field names.
   */
  public ArrayList<StringValue> getStaticFieldNames()
  {
    ArrayList<StringValue> names = new ArrayList<StringValue>();

    if (_staticFieldExprMap != null) {
      for (StringValue fieldName : _staticFieldNameMap.keySet()) {
        names.add(fieldName);
      }
    }

    return names;
  }

  /**
   * Adds a constant definition
   */
  public void addConstant(StringValue name, Expr expr)
  {
    _constMap.put(name, expr);
  }

  /**
   * Adds a constant definition
   */
  public void addJavaConstant(StringValue name, Object obj)
  {
    _constJavaMap.put(name, obj);
  }

  /**
   * Returns the number of fields.
   */
  public int getFieldSize()
  {
    return _fieldMap.size();
  }

  /**
   * Returns the name of the class that included this trait.
   */
  public String getTraitMethodBindingClassName(StringValue traitMethodName)
  {
    return _traitMethodBindingClassMap.get(traitMethodName);
  }

  public void validate(Env env)
  {
    if (! isAbstract() && ! isInterface() && ! isTrait()) {
      for (AbstractFunction fun : _methodMap.values()) {
        /* XXX: abstract methods need to be validated
              php/393g, php/393i, php/39j2
        if (! (absFun instanceof Function))
          continue;

        Function fun = (Function) absFun;
         */

        boolean isAbstract;

        // php/093g constructor
        if (_constructor != null
            && fun.getName().equals(_constructor.getName()))
          isAbstract = _constructor.isAbstract();
        else
          isAbstract = fun.isAbstract();

        if (isAbstract) {
          throw env.createErrorException(_classDef.getLocation(),
                                         L.l("Abstract function '{0}' must be implemented in concrete class {1}.",
                                             fun.getName(),
                                             getName()));
        }
      }
    }
  }

  public void init(Env env)
  {
    if (_staticFieldExprMap.size() == 0)
      return;

    for (Map.Entry<String,ArrayList<StaticField>> map
          : _staticFieldExprMap.entrySet()) {
      if (env.isInitializedClass(map.getKey()))
        continue;

      for (StaticField field : map.getValue()) {
        Value val;
        Expr expr = field._expr;

        //php/096f
        val = expr.eval(env);

        StringValue fullName = env.createStringBuilder();
        fullName.append(_className);
        fullName.append("::");
        fullName.append(field._name);

        env.setStaticRef(fullName, val);
      }

      env.addInitializedClass(map.getKey());
    }
  }

  @Override
  public Value getStaticFieldValue(Env env, StringValue name)
  {
    StringValue staticName = _staticFieldNameMap.get(name);

    if (staticName == null) {
      env.error(L.l("{0}::${1} is an undeclared static field",
                    _className, name));

      return NullValue.NULL;
    }

    return env.getStaticValue(staticName);
  }

  @Override
  public Var getStaticFieldVar(Env env, StringValue name)
  {
    StringValue staticName = _staticFieldNameMap.get(name);

    if (staticName == null) {
      env.error(L.l("{0}::${1} is an undeclared static field",
                    _className, name));

      throw new IllegalStateException();
    }

    return env.getStaticVar(staticName);
  }

  @Override
  public Value setStaticFieldRef(Env env, StringValue name, Value value)
  {
    StringValue staticName = _staticFieldNameMap.get(name);

    if (staticName == null) {
      env.error(L.l("{0}::{1} is an unknown static field",
                    _className, name));

      throw new IllegalStateException();
    }

    return env.setStaticRef(staticName, value);
  }

  /**
   * For Reflection.
   */
  public Value getStaticFieldInternal(Env env, StringValue name)
  {
    StringValue staticName = _staticFieldNameMap.get(name);

    if (staticName != null)
      return env.getStaticValue(staticName);
    else
      return null;
  }

  //
  // Constructors
  //

  /**
   * Creates a new instance.
   */
  /*
  public Value callNew(Env env, Expr []args)
  {
    Value object = _classDef.callNew(env, args);

    if (object != null)
      return object;

    object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null) {
      fun.callMethod(env, object, args);
    }

    return object;
  }
  */

  /**
   * Creates a new object without calling the constructor.  This is used
   * for unserializing classes.
   */
  public Value createObject(Env env)
  {
    if (isAbstract()) {
      throw env.createErrorException(L.l("abstract class '{0}' cannot be instantiated.",
                                         _className));
    }
    else if (isInterface()) {
      throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
                                         _className));
    }
    else if (isTrait()) {
      throw env.createErrorException(L.l("trait '{0}' cannot be instantiated.",
                                         _className));
    }

    ObjectValue objectValue = null;

    if (_isJavaWrapper) {
      // Java objects always need to call the constructor?
      return _javaClassDef.callNew(env, Value.NULL_ARGS);
    }
    else if (_javaClassDef != null && _javaClassDef.isDelegate()) {
      objectValue = new ObjectExtValue(this);
    }
    else if (_javaClassDef != null && _javaClassDef.isPhpClass()) {
      Object object = null;

      // Java objects always need to call the constructor?
      //Value javaWrapper = _javaClassDef.callNew(env, Value.NULL_ARGS);
      //object = javaWrapper.toJavaObject();

      objectValue = new ObjectExtJavaValue(this, object, _javaClassDef);
    }
    else if (_javaClassDef != null && ! _javaClassDef.isDelegate()) {
      objectValue = new ObjectExtJavaValue(this, null, _javaClassDef);
    }
    else {
      objectValue = _classDef.createObject(env, this);
    }

    initObject(env, objectValue);

    return objectValue;
  }

  /**
   * Initializes the object's methods and fields.
   */
  public void initObject(Env env, ObjectValue obj)
  {
    for (int i = 0; i < _initializers.size(); i++) {
      _initializers.get(i).initInstance(env, obj);
    }
  }

  public Value callNew(Env env, Object parentJavaObject, Value ...args)
  {
    QuercusClass oldCallingClass = env.setCallingClass(this);

    try {
      if (_classDef.isAbstract()) {
        throw env.createErrorException(L.l("abstract class '{0}' cannot be instantiated.",
                                           _className));
      }
      else if (_classDef.isInterface()) {
        throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
                                           _className));
      }
      else if (isTrait()) {
        throw env.createErrorException(L.l("trait '{0}' cannot be instantiated.",
                                           _className));
      }

      ObjectValue objectValue
        = new ObjectExtJavaValue(this, parentJavaObject, _javaClassDef);

      initObject(env, objectValue);

      AbstractFunction fun = findConstructor();

      // don't want to call the Java constructor to create another java object
      // php/0cl4, php/0cl5
      if (fun != null && ! fun.isJavaMethod()) {
        fun.callMethod(env, this, objectValue, args);
      }
      else {
        //  if expr
      }

      return objectValue;

    }
    finally {
      env.setCallingClass(oldCallingClass);
    }
  }

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Value ...args)
  {
    QuercusClass oldCallingClass = env.setCallingClass(this);

    try {
      if (isAbstract()) {
        throw env.createErrorException(L.l( "abstract class '{0}' cannot be instantiated.",
                                            _className));
      }
      else if (isInterface()) {
        throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
                                           _className));
      }
      else if (isTrait()) {
        throw env.createErrorException(L.l("trait '{0}' cannot be instantiated.",
                                           _className));
      }

      ObjectValue objectValue = null;

      if (_isJavaWrapper) {
        Value obj = _javaClassDef.callNew(env, args);

        return obj;
      }
      else if (_javaClassDef != null && _javaClassDef.isDelegate()) {
        objectValue = new ObjectExtValue(this);
      }
      else if (_javaClassDef != null && _javaClassDef.isPhpClass()) {
        Object object = null;

        // php/0k3-, php/0k4-
        //Value javaWrapper = _javaClassDef.callNew(env, args);
        //object = javaWrapper.toJavaObject();

        objectValue = new ObjectExtJavaValue(this, object, _javaClassDef);
      }
      else if (_javaClassDef != null && ! _javaClassDef.isDelegate()) {
        objectValue = new ObjectExtJavaValue(this, null, _javaClassDef);
      }
      else {
        objectValue = _classDef.newInstance(env, this);
      }

      initObject(env, objectValue);

      AbstractFunction fun = findConstructor();

      if (fun != null)
        fun.callNew(env, this, objectValue, args);
      else {
        //  if expr
      }

      return objectValue;
    }
    finally {
      env.setCallingClass(oldCallingClass);
    }
  }

  /**
   * Returns the parent class.
   */
  public String getParentName()
  {
    return _classDefList[0].getParentName();
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(String name)
  {
    return _instanceofSet.contains(name.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Returns an array of the interfaces that this class and its parents
   * implements.
   */
  public ArrayValue getInterfaces(Env env, boolean autoload)
  {
    ArrayValue array = new ArrayValueImpl();

    getInterfaces(env, array, autoload, true);

    return array;
  }

  /**
   * Puts the interfaces that this class and its parents implements
   * into the array.
   */
  private void getInterfaces(Env env, ArrayValue array,
                             boolean autoload, boolean isTop)
  {
    ClassDef [] defList = _classDefList;

    for (int i = 0; i < defList.length; i++) {
      ClassDef def = defList[i];

      if (! isTop && def.isInterface()) {
        String name = def.getName();

        array.put(name, name);
      }

      String []defNames = def.getInterfaces();

      for (int j = 0; j < defNames.length; j++) {
        QuercusClass cls = env.findClass(defNames[j]);

        cls.getInterfaces(env, array, autoload, false);
      }
    }

    if (_parent != null)
      _parent.getInterfaces(env, array, autoload, false);
  }

  /**
   * Returns true if this class or its parents implements specified interface.
   */
  public boolean implementsInterface(Env env, String name)
  {
    ClassDef [] defList = _classDefList;

    for (int i = 0; i < defList.length; i++) {
      ClassDef def = defList[i];

      if (def.isInterface() && def.getName().equals(name))
        return true;

      String []defNames = def.getInterfaces();

      for (int j = 0; j < defNames.length; j++) {
        QuercusClass cls = env.findClass(defNames[j]);

        if (cls.implementsInterface(env, name))
          return true;
      }
    }

    if (_parent != null)
      return _parent.implementsInterface(env, name);
    else
      return false;
  }

  /**
   * Finds the matching constructor.
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  //
  // Fields
  //

  /**
   * Implements the __get method call.
   * __get() is utilized for reading data from inaccessible properties.
   */
  public Value getField(Env env, Value qThis, StringValue name)
  {
    // php/09km, php/09kn
    // push/pop to prevent infinite recursion
    if(issetField(env, name) && _fieldMap.get(name).isPublic())
    {
        Value v_current = this.get(name); // TODO: move to ObjectExtValue if possible

        if(v_current != NullValue.NULL && v_current != UnsetValue.UNSET)
        {
            return v_current;
        }
        if(_fieldGet == null){
            return ((ClassField) _fieldMap.get(name)).getInitValue().eval(env);
        }
    }

    if (_fieldGet != null) {
      if (! env.pushFieldGet(Env.OVERLOADING_TYPES.FIELDGET, qThis.getClassName(), name))
        return UnsetValue.UNSET;

      try {
        return _fieldGet.callMethod(env, this, qThis, name);
      } finally {
        env.popFieldGet(Env.OVERLOADING_TYPES.FIELDGET);
      }
    }
    else
      return UnsetValue.UNSET;
  }

  /**
   * Implements the __isset method call.
   * __isset() is triggered by calling isset() or empty()  on inaccessible properties.
  */
  public boolean issetField(Env env, Value qThis, StringValue name)
  {
    ClassField field = _fieldMap.get(name);

    if (field != null && field.isPublic()) {
      return true;
    }

    if (_isset != null) {
      if (! env.pushFieldGet(Env.OVERLOADING_TYPES.ISSET, qThis.getClassName(), name))
        return false;

      try {
          Value result = _isset.callMethod(env, this, qThis, name);

          return result.toBoolean();
      }
      finally {
        env.popFieldGet(Env.OVERLOADING_TYPES.ISSET);
      }
    }

    return false;
  }

  @Override
  public boolean issetField(Env env, StringValue name) {
    if(_fieldMap.containsKey(name)) {
      return true;
    }

    return false;
  }

  @Override
  public void unsetField(StringValue name) {
    if(_fieldMap.containsKey(name))
      _fieldMap.remove(name);
  }

  /**
   * implements the __unset method call
   *  __unset() is invoked when unset() is used on inaccessible properties.
   */
  public Value unsetField(Env env, Value qThis, StringValue name)
  {
    if(issetField(env, name) && _fieldMap.get(name).isPublic()){
      // TODO: move to ObjectExtValue if possible
      unsetField(name);
      return NullValue.NULL;
    }

    // basically a copy of the __get code with slightly different semantics
    if (_unset != null) {
      if (! env.pushFieldGet(Env.OVERLOADING_TYPES.UNSET, qThis.getClassName(), name))
        return UnsetValue.UNSET;

      try {
        return _unset.callMethod(env, this, qThis, name);
      } finally {
        env.popFieldGet(Env.OVERLOADING_TYPES.UNSET);
      }
    }
    else
      unsetField(name);

    return NullValue.NULL;
  }

  /**
   * Implements the __set method call.
   */
  public Value setField(Env env, Value qThis, StringValue name, Value value)
  {
    if (_fieldSet != null){
        if (! env.pushFieldGet(Env.OVERLOADING_TYPES.FIELDSET, qThis.getClassName(), name))
          return UnsetValue.UNSET;

        try {
            return _fieldSet.callMethod(env, this, qThis, name, value);
        }
        finally {
            env.popFieldGet(Env.OVERLOADING_TYPES.FIELDSET);
        }
    }
    return UnsetValue.UNSET;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findStaticFunction(String name)
  {
    return findFunction(name);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(StringValue methodName)
  {
    return _methodMap.get(methodName, methodName.hashCodeCaseInsensitive());
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction findFunction(String methodName)
  {
    StringValue nameV = _moduleContext.createString(methodName);

    return _methodMap.getRaw(nameV);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction findFunction(StringValue methodName)
  {
    return _methodMap.getRaw(methodName);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(StringValue methodName, int hash)
  {
    return _methodMap.get(methodName, methodName.hashCode());
  }

  /**
   * Calls the A::__construct constructor.
   */
  public Value callConstructor(Env env, Value qThis, Value ...args)
  {
    AbstractFunction cons = getConstructor();

    if (cons == null) {
      env.error(L.l("cannot call constructor for class {0}", getName()));
    }
    else if (qThis.isNull()) {
      env.error(L.l("{0}::{1}() cannot be called statically",
                    getName(),
                    cons.getName()));
    }

    return getConstructor().callMethod(env, this, qThis, args);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value qThis,
                          StringValue methodName, int hash,
                          Value []args)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, args);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value []args)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      args);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive());
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash,
                          Value a1)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, a1);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value a1)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      a1);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash,
                          Value a1, Value a2)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value a1, Value a2)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      a1, a2);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value a1, Value a2, Value a3)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      a1, a2, a3);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3, a4);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value a1, Value a2, Value a3, Value a4)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      a1, a2, a3, a4);
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value qThis,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3, a4, a5);
  }

  public final Value callMethod(Env env, Value qThis, StringValue methodName,
                                Value a1, Value a2, Value a3, Value a4,
                                Value a5)
  {
    return callMethod(env, qThis,
                      methodName, methodName.hashCodeCaseInsensitive(),
                      a1, a2, a3, a4, a5);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value []args)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis, args);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value []args)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         args);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive());
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value a1)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value a1)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         a1);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value a1, Value a2)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value a1, Value a2)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         a1, a2);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2, a3);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value a1, Value a2, Value a3)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         a1, a2, a3);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis,
                             a1, a2, a3, a4);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value a1, Value a2, Value a3, Value a4)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         a1, a2, a3, a4);
  }

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value qThis,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    if (qThis.isNull())
      qThis = this;

    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, this, qThis,
                             a1, a2, a3, a4, a5);
  }

  public final Value callMethodRef(Env env, Value qThis, StringValue methodName,
                                   Value a1, Value a2, Value a3, Value a4,
                                   Value a5)
  {
    return callMethodRef(env, qThis,
                         methodName, methodName.hashCodeCaseInsensitive(),
                         a1, a2, a3, a4, a5);
  }

  //
  // Static method calls
  //

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value []args)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            args);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive());
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value a1)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            a1);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value a1, Value a2)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            a1, a2);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value a1, Value a2, Value a3)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            a1, a2, a3);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value a1, Value a2, Value a3, Value a4)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            a1, a2, a3, a4);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName,
                                Value a1, Value a2, Value a3, Value a4,
                                Value a5)
  {
    return callStaticMethod(env, qThis,
                            methodName, methodName.hashCodeCaseInsensitive(),
                            a1, a2, a3, a4, a5);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value []args)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, args);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value a1)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, a1);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value a1, Value a2)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value a1, Value a2, Value a3)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value a1, Value a2, Value a3, Value a4)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3, a4);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethod(Env env,
                                Value qThis,
                                StringValue methodName, int hash,
                                Value a1, Value a2, Value a3, Value a4,
                                Value a5)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethod(env, this, qThis, a1, a2, a3, a4, a5);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value []args)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               args);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive());
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value a1)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               a1);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value a1, Value a2)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               a1, a2);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value a1, Value a2, Value a3)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               a1, a2, a3);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value a1, Value a2, Value a3, Value a4)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               a1, a2, a3, a4);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName,
                                   Value a1, Value a2, Value a3, Value a4,
                                   Value a5)
  {
    return callStaticMethodRef(env, qThis,
                               methodName, methodName.hashCodeCaseInsensitive(),
                               a1, a2, a3, a4, a5);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value []args)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, args);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value a1)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value a1, Value a2)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value a1, Value a2, Value a3)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2, a3);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value a1, Value a2, Value a3, Value a4)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2, a3, a4);
  }

  /**
   * calls the function statically.
   */
  public Value callStaticMethodRef(Env env,
                                   Value qThis,
                                   StringValue methodName, int hash,
                                   Value a1, Value a2, Value a3, Value a4,
                                   Value a5)
  {
    if (qThis.isNull()) {
      qThis = this;
    }

    AbstractFunction fun = _methodMap.getStatic(methodName, hash);

    return fun.callMethodRef(env, this, qThis, a1, a2, a3, a4, a5);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value []args)
  {
    return callMethod(env, this, methodName, hash, args);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash)
  {
    return callMethod(env, this, methodName, hash);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1)
  {
    return callMethod(env, this, methodName, hash,
                      a1);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2)
  {
    return callMethod(env, this, methodName, hash,
                      a1, a2);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3)
  {
    return callMethod(env, this, methodName, hash,
                      a1, a2, a3);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return callMethod(env, this, methodName, hash,
                      a1, a2, a3, a4);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4,
                          Value a5)
  {
    return callMethod(env, this, methodName, hash,
                      a1, a2, a3, a4, a5);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value []args)
  {
    return callMethodRef(env, this, methodName, hash, args);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash)
  {
    return callMethodRef(env, this, methodName, hash);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value a1)
  {
    return callMethodRef(env, this, methodName, hash,
                         a1);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value a1, Value a2)
  {
    return callMethodRef(env, this, methodName, hash,
                         a1, a2);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3)
  {
    return callMethodRef(env, this, methodName, hash,
                         a1, a2, a3);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4)
  {
    return callMethodRef(env, this, methodName, hash,
                         a1, a2, a3, a4);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethodRef(Env env,
                             StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4,
                             Value a5)
  {
    return callMethodRef(env, this, methodName, hash,
                         a1, a2, a3, a4, a5);
  }

  private String toMethod(char []key, int keyLength)
  {
    return new String(key, 0, keyLength);
  }

  /**
   * Finds a function.
   */
  public AbstractFunction findStaticFunctionLowerCase(String name)
  {
    return null;
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getStaticFunction(String name)
  {
    AbstractFunction fun = findStaticFunction(name);
    /*
    if (fun != null)
      return fun;

    fun = findStaticFunctionLowerCase(name.toLowerCase(Locale.ENGLISH));
    */

    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
                                            getName(), name));
    }
  }

  /**
   * Finds the matching constant
   */
  public final Value getConstant(Env env, StringValue name)
  {
    Expr expr = _constMap.get(name);

    if (expr != null)
      return expr.eval(env);

    Object obj = _constJavaMap.get(name);

    if (obj != null)
      return env.wrapJava(obj);

    throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown constant",
                                          getName(), name));
  }

  /**
   * Returns true if the constant exists.
   */
  public final boolean hasConstant(StringValue name)
  {
    if (_constMap.get(name) != null)
      return true;
    else
      return _constJavaMap.get(name) != null;
  }

  /**
   * Returns the constants defined in this class.
   */
  public final HashMap<StringValue, Value> getConstantMap(Env env)
  {
    HashMap<StringValue, Value> map = new HashMap<StringValue, Value>();

    for (Map.Entry<StringValue, Expr> entry : _constMap.entrySet()) {
      map.put(entry.getKey(), entry.getValue().eval(env));
    }

    for (Map.Entry<StringValue, Object> entry : _constJavaMap.entrySet()) {
      map.put(entry.getKey(), env.wrapJava(entry.getValue()));
    }

    return map;
  }

  //
  // Value methods
  //

  @Override
  public boolean isNull()
  {
    return false;
  }

  /**
   * Returns the value's class name.
   */
  @Override
  public String getClassName()
  {
    return getName();
  }

  @Override
  public QuercusClass getQuercusClass()
  {
    return this;
  }

  @Override
  public QuercusClass findQuercusClass(Env env)
  {
    return this;
  }

  public int hashCode()
  {
    return _className.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof QuercusClass))
      return false;

    QuercusClass qClass = (QuercusClass) o;

    if (_classDef != qClass._classDef)
      return false;

    if (_javaClassDef != qClass._javaClassDef)
      return false;

    if (_parent == qClass._parent)
      return true;

    else
      return (_parent != null && _parent.equals(qClass._parent));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }

  static class StaticField
  {
    StringValue _name;
    Expr _expr;

    StaticField(StringValue name, Expr expr)
    {
      _name = name;
      _expr = expr;
    }

    StringValue getName()
    {
      return _name;
    }
  }

  public void setIsset(AbstractFunction isset)
  {
    _isset = isset;
  }

  public void setUnset(AbstractFunction unset)
  {
    _unset = unset;
  }

  public AbstractFunction getIsset()
  {
    return _isset;
  }

  public AbstractFunction getUnset()
  {
    return _unset;
  }
}


/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.program;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.FieldVisibility;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.Location;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents an interpreted PHP class definition.
 */
public class InterpretedClassDef extends ClassDef
  implements InstanceInitializer
{
  protected boolean _isAbstract;
  protected boolean _isInterface;
  protected boolean _isTrait;
  protected boolean _isFinal;

  protected boolean _hasPublicMethods;
  protected boolean _hasProtectedMethods;
  protected boolean _hasPrivateMethods;

  // true if defined in the top scope of a page
  private boolean _isTopScope;

  protected final LinkedHashMap<StringValue,AbstractFunction> _functionMap
    = new LinkedHashMap<StringValue,AbstractFunction>();

  protected final LinkedHashMap<StringValue,ClassField> _fieldMap
    = new LinkedHashMap<StringValue,ClassField>();

  protected final LinkedHashMap<StringValue,StaticFieldEntry> _staticFieldMap
    = new LinkedHashMap<StringValue,StaticFieldEntry>();

  protected final HashMap<StringValue,Expr> _constMap
    = new HashMap<StringValue,Expr>();

  protected AbstractFunction _constructor;
  protected AbstractFunction _destructor;
  protected AbstractFunction _getField;
  protected AbstractFunction _setField;
  protected AbstractFunction _isset;
  protected AbstractFunction _unset;
  protected AbstractFunction _call;
  protected AbstractFunction _callStatic;

  protected AbstractFunction _serializeFun;
  protected AbstractFunction _unserializeFun;

  protected AbstractFunction _invoke;
  protected AbstractFunction _toString;

  protected int _parseIndex;

  protected String _comment;

  public InterpretedClassDef(Location location,
                             String name,
                             String parentName,
                             String []ifaceList,
                             int index)
  {
    this(location,
          name, parentName, ifaceList, ClassDef.NULL_STRING_ARRAY, index);
  }

  public InterpretedClassDef(Location location,
                             String name,
                             String parentName,
                             String []ifaceList,
                             String []traitList,
                             int index)
  {
    super(location, name, parentName, ifaceList, traitList);

    _parseIndex = index;
  }

  public InterpretedClassDef(String name,
                             String parentName,
                             String []ifaceList,
                             String []traitList)
  {
    this(null, name, parentName, ifaceList, traitList, 0);
  }

  public InterpretedClassDef(String name,
                             String parentName,
                             String []ifaceList)
  {
    this(null, name, parentName, ifaceList, ClassDef.NULL_STRING_ARRAY, 0);
  }

  /**
   * true for an abstract class.
   */
  public void setAbstract(boolean isAbstract)
  {
    _isAbstract = isAbstract;
  }

  /**
   * True for an abstract class.
   */
  @Override
  public boolean isAbstract()
  {
    return _isAbstract;
  }

  /**
   * true for an interface class.
   */
  public void setInterface(boolean isInterface)
  {
    _isInterface = isInterface;
  }

  /**
   * True for an interface class.
   */
  @Override
  public boolean isInterface()
  {
    return _isInterface;
  }

  /**
   * true for an trait class.
   */
  public void setTrait(boolean isTrait)
  {
    _isTrait = isTrait;
  }

  /**
   * True for an trait class.
   */
  @Override
  public boolean isTrait()
  {
    return _isTrait;
  }

  /**
   * True for a final class.
   */
  public void setFinal(boolean isFinal)
  {
    _isFinal = isFinal;
  }

  /**
   * Returns true for a final class.
   */
  public boolean isFinal()
  {
    return _isFinal;
  }

  /**
   * Returns true if class has public methods.
   */
  public boolean hasPublicMethods()
  {
    return _hasPublicMethods;
  }

  /**
   * Returns true if class has protected or private methods.
   */
  public boolean hasProtectedMethods()
  {
    return _hasProtectedMethods;
  }

  /**
   * Returns true if the class has private methods.
   */
  public boolean hasPrivateMethods()
  {
    return _hasPrivateMethods;
  }

  /**
   * True if defined at the top-level scope
   */
  public boolean isTopScope()
  {
    return _isTopScope;
  }

  /**
   * True if defined at the top-level scope
   */
  public void setTopScope(boolean isTopScope)
  {
    _isTopScope = isTopScope;
  }

  /**
   * Unique name to use for compilation.
   */
  public String getCompilationName()
  {
    String name = getName();
    name = name.replace("__", "___");
    name = name.replace("\\", "__");

    return name + "_" + _parseIndex;
  }

  /**
   * Unique instance name for the compiled class.
   */
  public String getCompilationInstanceName()
  {
    return "q_cl_" + getCompilationName();
  }

  /**
   * Initialize the quercus class methods.
   */
  @Override
  public void initClassMethods(QuercusClass cl, String bindingClassName)
  {
    cl.addInitializer(this);

    if (_constructor != null) {
      cl.setConstructor(_constructor);

      // php/093o
      //if (_functionMap.get("__construct") == null) {
      //  cl.addMethod("__construct", _constructor);
      //}
    }

    if (_destructor != null) {
      cl.setDestructor(_destructor);

      // XXX: make sure we need to do this (test case), also look at ProClassDef
      cl.addMethod(cl.getModuleContext().createString("__destruct"), _destructor);
    }

    if (_getField != null)
      cl.setFieldGet(_getField);

    if (_setField != null)
      cl.setFieldSet(_setField);

    if (_call != null) {
      cl.setCall(_call);
    }

    if (_callStatic != null) {
      cl.setCallStatic(_callStatic);
    }

    if (_invoke != null)
      cl.setInvoke(_invoke);

    if (_toString != null)
      cl.setToString(_toString);

    if (_isset != null)
      cl.setIsset(_isset);

    if (_unset != null)
      cl.setUnset(_unset);

    if (_serializeFun != null) {
      cl.setSerialize(_serializeFun, _unserializeFun);
    }

    for (Map.Entry<StringValue,AbstractFunction> entry : _functionMap.entrySet()) {
      StringValue funName = entry.getKey();
      AbstractFunction fun = entry.getValue();

      if (fun.isTraitMethod()) {
        cl.addTraitMethod(bindingClassName, funName, fun);
      }
      else {
        cl.addMethod(funName, fun);
      }
    }
  }

  /**
   * Initialize the quercus class fields.
   */
  @Override
  public void initClassFields(QuercusClass cl, String declaringClassName)
  {
    if (isTrait()) {
      for (Map.Entry<StringValue,ClassField> entry : _fieldMap.entrySet()) {
        ClassField field = entry.getValue();

        cl.addTraitField(field);
      }
    }
    else {
      for (Map.Entry<StringValue,ClassField> entry : _fieldMap.entrySet()) {
        ClassField field = entry.getValue();

        cl.addField(field);
      }
    }

    if (isTrait()) {
      for (Map.Entry<StringValue, StaticFieldEntry> entry : _staticFieldMap.entrySet()) {
        StaticFieldEntry field = entry.getValue();

        cl.addStaticTraitFieldExpr(declaringClassName,
                                   entry.getKey(), field.getValue());
      }
    }
    else {
      String className = getName();
      for (Map.Entry<StringValue, StaticFieldEntry> entry : _staticFieldMap.entrySet()) {
        StaticFieldEntry field = entry.getValue();

        cl.addStaticFieldExpr(className, entry.getKey(), field.getValue());
      }
    }

    for (Map.Entry<StringValue, Expr> entry : _constMap.entrySet()) {
      cl.addConstant(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }

  @Override
  public AbstractFunction getCall()
  {
    return _call;
  }

  @Override
  public AbstractFunction getCallStatic()
  {
    return _callStatic;
  }

  @Override
  public AbstractFunction getSerialize()
  {
    return _serializeFun;
  }

  @Override
  public AbstractFunction getUnserialize()
  {
    return _unserializeFun;
  }

  /**
   * Adds a function.
   */
  public void addFunction(StringValue name, Function fun)
  {
    _functionMap.put(name, fun);

    if (fun.isPublic()) {
      _hasPublicMethods = true;
    }

    if (fun.isProtected()) {
      _hasProtectedMethods = true;
    }

    if (fun.isPrivate()) {
      _hasPrivateMethods = true;
    }

    if (name.equalsString("__construct")) {
      _constructor = fun;
    }
    else if (name.equalsString("__destruct")) {
      _destructor = fun;
    }
    else if (name.equalsString("__get")) {
      _getField = fun;
    }
    else if (name.equalsString("__set")) {
      _setField = fun;
    }
    else if (name.equalsString("__call")) {
      _call = fun;
    }
    else if (name.equalsString("__callStatic")) {
      _callStatic = fun;
    }
    else if (name.equalsString("__invoke")) {
      _invoke = fun;
    }
    else if (name.equalsString("__toString")) {
      _toString = fun;
    }
    else if (name.equalsString("__isset")) {
      _isset = fun;
    }
    else if (name.equalsString("__unset")) {
      _unset = fun;
    }
    else if (name.equalsStringIgnoreCase(getName()) && _constructor == null) {
      _constructor = fun;
    }
    else if (name.equalsString("serialize") && isA(null, "Serializable")) {
      _serializeFun = fun;
    }
    else if (name.equalsString("unserialize") && isA(null, "Serializable")) {
      _unserializeFun = fun;
    }
  }

  /**
   * Adds a static value.
   */
  public void addStaticValue(Value name, Expr value)
  {
    _staticFieldMap.put(name.toStringValue(), new StaticFieldEntry(value));
  }

  /**
   * Adds a static value.
   */
  public void addStaticValue(Value name, Expr value, String comment)
  {
    _staticFieldMap.put(name.toStringValue(), new StaticFieldEntry(value, comment));
  }

  /**
   * Adds a const value.
   */
  public void addConstant(StringValue name, Expr value)
  {
    _constMap.put(name, value);
  }

  /**
   * Return a const value.
   */
  public Expr findConstant(String name)
  {
    return _constMap.get(name);
  }

  /**
   * Adds a value.
   */
  public void addClassField(StringValue name,
                            Expr value,
                            FieldVisibility visibility,
                            String comment)
  {
    ClassField field = new ClassField(name,
                                      getName(),
                                      value,
                                      visibility,
                                      comment,
                                      isTrait());

    _fieldMap.put(name, field);
  }

  /**
   * Adds a value.
   */
  public ClassField getClassField(StringValue name)
  {
    ClassField field = _fieldMap.get(name);

    return field;
  }

  /**
   * Return true for a declared field.
   */
  public boolean isDeclaredField(StringValue name)
  {
    return _fieldMap.get(name) != null;
  }

  /**
   * Initialize the class.
   */
  public void init(Env env)
  {
    QuercusClass qClass = env.getClass(getName());

    for (Map.Entry<StringValue,StaticFieldEntry> entry : _staticFieldMap.entrySet()) {
      StringValue name = entry.getKey();

      StaticFieldEntry field = entry.getValue();

      Var var = qClass.getStaticFieldVar(env, name);

      var.set(field.getValue().eval(env).copy());
    }
  }

  /**
   * Initialize the fields
   */
  @Override
  public void initInstance(Env env, Value obj, boolean isInitFieldValues)
  {
    ObjectValue object = (ObjectValue) obj;

    for (Map.Entry<StringValue,ClassField> entry : _fieldMap.entrySet()) {
      ClassField field = entry.getValue();

      object.initField(env, field, isInitFieldValues);
    }

    if (_destructor != null) {
      env.addObjectCleanup(object);
    }
  }

  /**
   * Returns the constructor
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  /**
   * Sets the documentation for this class.
   */
  public void setComment(String comment)
  {
    _comment = comment;
  }

  /**
   * Returns the documentation for this class.
   */
  @Override
  public String getComment()
  {
    return _comment;
  }

  /**
   * Returns the comment for the specified field.
   */
  @Override
  public String getFieldComment(StringValue name)
  {
    ClassField field = _fieldMap.get(name);

    if (field != null)
      return field.getComment();
    else
      return null;
  }

  /**
   * Returns the comment for the specified field.
   */
  @Override
  public String getStaticFieldComment(StringValue name)
  {
    StaticFieldEntry field = _staticFieldMap.get(name);

    if (field != null)
      return field.getComment();
    else
      return null;
  }

  @Override
  public Set<Map.Entry<StringValue,ClassField>> fieldSet()
  {
    return _fieldMap.entrySet();
  }

  @Override
  public ClassField getField(StringValue name)
  {
    return _fieldMap.get(name);
  }

  @Override
  public Set<Map.Entry<StringValue, StaticFieldEntry>> staticFieldSet()
  {
    return _staticFieldMap.entrySet();
  }

  @Override
  public Set<Map.Entry<StringValue, AbstractFunction>> functionSet()
  {
    return _functionMap.entrySet();
  }

  public AbstractFunction getFunction(StringValue name)
  {
    return _functionMap.get(name);
  }

  public HashMap<StringValue,AbstractFunction> getFunctionMap()
  {
    return _functionMap;
  }
}


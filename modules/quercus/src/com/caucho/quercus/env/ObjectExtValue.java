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

import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a PHP object value.
 */
@SuppressWarnings("serial")
public class ObjectExtValue extends ObjectValue
  implements Serializable
{
  private MethodMap<AbstractFunction> _methodMap;

  private LinkedHashMap<StringValue,Entry> _fieldMap
    = new LinkedHashMap<StringValue,Entry>();

  private Entry _head;

  private boolean _isFieldInit;

  public ObjectExtValue(QuercusClass cl)
  {
    super(cl);

    _methodMap = cl.getMethodMap();
  }

  public ObjectExtValue(Env env, ObjectExtValue copy, CopyRoot root)
  {
    super(copy.getQuercusClass());

    root.putCopy(copy, this);

    _methodMap = copy._methodMap;

    _isFieldInit = copy._isFieldInit;

    for (Map.Entry<StringValue,Entry> entry : copy._fieldMap.entrySet()) {
      Entry entryCopy = entry.getValue().copyTree(env, root);

      _fieldMap.put(entry.getKey(), entryCopy);
    }

    _incompleteObjectName = copy._incompleteObjectName;
  }

  public ObjectExtValue(Env env,
                        IdentityHashMap<Value,Value> copyMap,
                        ObjectExtValue copy)
  {
    super(copy.getQuercusClass());

    _methodMap = copy._methodMap;

    _isFieldInit = copy._isFieldInit;

    for (Map.Entry<StringValue,Entry> entry : copy._fieldMap.entrySet()) {
      Entry entryCopy = new Entry(env, copyMap, entry.getValue());

      _fieldMap.put(entry.getKey(), entryCopy);
    }

    _incompleteObjectName = copy._incompleteObjectName;
  }

  private void init()
  {
    _fieldMap = new LinkedHashMap<StringValue,Entry>();
  }

  @Override
  protected void setQuercusClass(QuercusClass cl)
  {
    super.setQuercusClass(cl);

    _methodMap = cl.getMethodMap();
  }

  /**
   * Initializes the incomplete class.
   */
  @Override
  public void initObject(Env env, QuercusClass cls)
  {
    setQuercusClass(cls);
    _incompleteObjectName = null;

    LinkedHashMap<StringValue,Entry> existingFields = _fieldMap;
    _fieldMap = new LinkedHashMap<StringValue,Entry>();

    cls.initObject(env, this);

    Iterator<Entry> iter = existingFields.values().iterator();

    while (iter.hasNext()) {
      Entry newField = iter.next();

      Entry entry = getThisEntry(newField._key);

      if (entry != null)
        entry._value = newField._value;
      else
        initField(newField._key, newField._value, newField._visibility);
    }
  }

  /**
   * Returns the number of entries.
   */
  @Override
  public int getSize()
  {
    return _fieldMap.size();
  }

  /**
   * Gets a field value.
   */
  @Override
  public final Value getField(Env env, StringValue name)
  {
    Value returnValue = getFieldExt(env, name);

    if (returnValue == UnsetValue.UNSET) {
      // __get didn't work, lets look in the class itself
      Entry entry = _fieldMap.get(name);

      if (entry != null) {
        // php/09ks vs php/091m
        return entry._value.toValue();
      }
    }

    return returnValue;
  }

  /**
   * Gets a field value.
   */
  @Override
  public Value getThisField(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    if (entry != null) {
      return entry._value.toValue();
    }

    return getFieldExt(env, name);
  }

  /**
   * Returns fields not explicitly specified by this value.
   */
  protected Value getFieldExt(Env env, StringValue name)
  {
    Entry e = this.getEntry(env, name);

    if(e != null && e._value != NullValue.NULL && e._value != UnsetValue.UNSET) {
      return e._value;
    }

    return _quercusClass.getField(env, this, name);
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getFieldVar(Env env, StringValue name)
  {
    Entry entry = getEntry(env, name);

    if (entry != null) {
      Value value = entry._value;

      if (value instanceof Var)
        return (Var) value;

      Var var = new Var(value);
      entry._value = var;

      return var;
    }

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET) {
      if (value instanceof Var)
        return (Var) value;
      else
        return new Var(value);
    }

    // php/3d28
    entry = createEntry(name, FieldVisibility.PUBLIC);

    value = entry._value;

    if (value instanceof Var)
      return (Var) value;

    Var var = new Var(value);

    entry.setValue(var);

    return var;
  }

  /**
   * Returns the array ref.
   */
  @Override
  public Var getThisFieldVar(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    if (entry != null) {
      Value value = entry._value;

      if (value instanceof Var)
        return (Var) value;

      Var var = new Var(value);
      entry._value = var;

      return var;
    }

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET) {
      if (value instanceof Var)
        return (Var) value;
      else
        return new Var(value);
    }

    entry = createEntry(name, FieldVisibility.PUBLIC);

    value = entry._value;

    if (value instanceof Var)
      return (Var) value;

    Var var = new Var(value);

    entry.setValue(var);

    return var;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getFieldArg(Env env, StringValue name, boolean isTop)
  {
    Entry entry = getEntry(env, name);

    if (entry != null) {
      Value value = entry.getValue();

      if (isTop || ! value.isset())
        return entry.toArg();
      else
        return value;
    }

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET)
      return value;

    return new ArgGetFieldValue(env, this, name);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getThisFieldArg(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    if (entry != null)
      return entry.toArg();

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET)
      return value;

    return new ArgGetFieldValue(env, this, name);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getFieldArgRef(Env env, StringValue name)
  {
    Entry entry = getEntry(env, name);

    if (entry != null)
      return entry.toArg();

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET)
      return value;

    return new ArgGetFieldValue(env, this, name);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getThisFieldArgRef(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    if (entry != null)
      return entry.toArg();

    Value value = getFieldExt(env, name);

    if (value != UnsetValue.UNSET)
      return value;

    return new ArgGetFieldValue(env, this, name);
  }

  /**
   * Adds a new value.
   */
  @Override
  public Value putField(Env env, StringValue name, Value value)
  {
    Entry entry = getEntry(env, name);

    // XXX: php/09ks, need visibility check
    if (entry == null) {
      Value oldValue = putFieldExt(env, name, value);

      if (oldValue != null)
        return oldValue;

      if (! _isFieldInit) {
        AbstractFunction fieldSet = _quercusClass.getFieldSet();

        if (fieldSet != null) {
          _isFieldInit = true;
          Value retVal = _quercusClass.setField(env, this, name, value);
          _isFieldInit = false;
          if(retVal != UnsetValue.UNSET)
            return retVal;
        }
      }

      entry = createEntry(name, FieldVisibility.PUBLIC);
    }

    Value oldValue = entry._value;

    if (value instanceof Var) {
      Var var = (Var) value;

      // for function return optimization
      // var.setReference();

      entry._value = var;
    }
    else if (oldValue instanceof Var) {
      oldValue.set(value);
    }
    else {
      entry._value = value;
    }

    return value;
  }

  /**
   * Sets/adds field to this object.
   */
  @Override
  public Value putThisField(Env env, StringValue name, Value value)
  {
    Entry entry = getThisEntry(name);

    if (entry == null) {
      Value oldValue = putFieldExt(env, name, value);

      if (oldValue != null)
        return oldValue;

      if (! _isFieldInit) {
        AbstractFunction fieldSet = _quercusClass.getFieldSet();

        if (fieldSet != null) {
          //php/09k7
          _isFieldInit = true;
          Value retValue = NullValue.NULL;

          try {
            retValue = fieldSet.callMethod(env,
                                           _quercusClass,
                                           this,
                                           name,
                                           value);
          } finally {
            _isFieldInit = false;
          }

          return retValue;
        }
      }
    }

    entry = createEntry(name, FieldVisibility.PUBLIC);

    Value oldValue = entry._value;

    if (value instanceof Var) {
      Var var = (Var) value;

      // for function return optimization
      // var.setReference();

      entry._value = var;
    }
    else if (oldValue instanceof Var) {
      oldValue.set(value);
    }
    else {
      entry._value = value;
    }

    return value;
  }

  protected Value putFieldExt(Env env, StringValue name, Value value)
  {
    return null;
  }

  @Override
  public void setFieldInit(boolean isInit)
  {
    _isFieldInit = isInit;
  }

  /**
   * Returns true if the object is in a __set() method call.
   * Prevents infinite recursion.
   */
  @Override
  public boolean isFieldInit()
  {
    return _isFieldInit;
  }

  /**
   * Adds a new value to the object.
   */
  @Override
  public void initField(StringValue key,
                        Value value,
                        FieldVisibility visibility)
  {
    Entry entry = createEntry(key, visibility);

    entry._value = value;
  }

  /**
   * Removes a value.
   */
  @Override
  public void unsetField(StringValue name)
  {
    Value returnValue = _quercusClass.unsetField(Env.getCurrent(),this,name);
    if(returnValue == UnsetValue.UNSET || returnValue == NullValue.NULL) {
      // __unset didn't work, lets look in the class itself

      _fieldMap.remove(name);
    }

    return;

  }


  /**
   * Removes the field array ref.
   */
  @Override
  public void unsetArray(Env env, StringValue name, Value index)
  {
    // php/022b
    if (_quercusClass.getFieldGet() != null)
      return;

    Entry entry = createEntry(name, FieldVisibility.PUBLIC);

    // XXX
    //if (entry._visibility == FieldVisibility.PRIVATE)
      //return;

    entry.toValue().remove(index);
  }

  /**
   * Removes the field array ref.
   */
  public void unsetThisArray(Env env, StringValue name, Value index)
  {
    if (_quercusClass.getFieldGet() != null)
      return;

    Entry entry = createEntry(name, FieldVisibility.PUBLIC);

    entry.toValue().remove(index);
  }

  /**
   * Gets a new value.
   */
  private Entry getEntry(Env env, StringValue name)
  {
    Entry entry = _fieldMap.get(name);

    if (entry == null) {
      return null;
    }

    if (entry._visibility == FieldVisibility.PRIVATE) {
      QuercusClass cls = env.getCallingClass();

      // XXX: this really only checks access from outside of class scope
      // php/091m
      if (cls != _quercusClass) {
        return null;
      }
      /* nam: 2012-04-29 this doesn't work, commented out for drupal-7.12
      else if (entry._visibility == FieldVisibility.PROTECTED) {
        QuercusClass cls = env.getCallingClass();

        if (cls == null || (cls != _quercusClass && ! cls.isA(_quercusClass.getName()))) {
            env.notice(L.l("Can't access protected field '{0}::${1}'",
                           _quercusClass.getName(), name));

            return null;
        }
      }
      */
    }

    return entry;
  }

  /**
   * Gets a new value.
   */
  private Entry getThisEntry(StringValue name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Creates the entry for a key.
   */
  private Entry createEntry(StringValue name, FieldVisibility visibility)
  {
    Entry entry = _fieldMap.get(name);

    if (entry == null) {
      entry = new Entry(name, visibility);
      _fieldMap.put(name, entry);
    }

    return entry;
  }

  //
  // Foreach/Traversable functions
  //

  /**
   * Returns an iterator for the key => value pairs.
   */
  @Override
  public Iterator<Map.Entry<Value, Value>> getIterator(Env env)
  {
    TraversableDelegate delegate = _quercusClass.getTraversableDelegate();

    if (delegate != null)
      return delegate.getIterator(env, this);
    else
      return getBaseIterator(env);
  }

  /**
   * Returns an iterator for the key => value pairs.
   */
  @Override
  public Iterator<Map.Entry<Value, Value>> getBaseIterator(Env env)
  {
    return new KeyValueIterator(_fieldMap.values().iterator());
  }

  /**
   * Returns an iterator for the keys.
   */
  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    TraversableDelegate delegate = _quercusClass.getTraversableDelegate();

    if (delegate != null)
      return delegate.getKeyIterator(env, this);

    return new KeyIterator(_fieldMap.keySet().iterator());
  }

  /**
   * Returns an iterator for the values.
   */
  @Override
  public Iterator<Value> getValueIterator(Env env)
  {
    TraversableDelegate delegate = _quercusClass.getTraversableDelegate();

    if (delegate != null)
      return delegate.getValueIterator(env, this);

    return new ValueIterator(_fieldMap.values().iterator());
  }

  //
  // method calls
  //

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value []args)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, StringValue methodName, int hash,
                          Value a1, Value a2)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, a1, a2);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, a1, a2, a3);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, a1, a2, a3, a4);
  }

  /**
   * calls the function.
   */
  @Override
  public Value callMethod(Env env,
                          StringValue methodName, int hash,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethod(env, _quercusClass, this, a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value []args)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, StringValue methodName, int hash,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    AbstractFunction fun = _methodMap.get(methodName, hash);

    return fun.callMethodRef(env, _quercusClass, this, a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method.
   */
  /*
  @Override
  public Value callClassMethod(Env env, AbstractFunction fun, Value []args)
  {
    return fun.callMethod(env, this, args);
  }
  */

  /**
   * Returns the value for the variable, creating an object if the var
   * is unset.
   */
  @Override
  public Value getObject(Env env)
  {
    return this;
  }

  /*
  @Override
  public Value getObject(Env env, Value index)
  {
    // php/3d92

    env.error(L.l("Can't use object '{0}' as array", getName()));

    return NullValue.NULL;
  }
  */

  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    return this;
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    Value oldValue = map.get(this);

    if (oldValue != null)
      return oldValue;

    // php/4048 - needs to be deep copy

    return new ObjectExtValue(env, map, this);
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copyTree(Env env, CopyRoot root)
  {
    // php/420c

    Value copy = root.getCopy(this);

    if (copy != null)
      return copy;
    else
      return new CopyObjectExtValue(env, this, root);
  }

  /**
   * Clone the object
   */
  @Override
  public Value clone(Env env)
  {
    ObjectExtValue newObject = new ObjectExtValue(_quercusClass);

    clone(env, newObject);

    return newObject;
  }

  protected void clone(Env env, ObjectExtValue obj) {
    _quercusClass.initObject(env, obj);

    Iterator<Entry> iter = _fieldMap.values().iterator();

    while (iter.hasNext()) {
      Entry entry = iter.next();

      Entry copy = obj.createEntry(entry.getKey(),
                                   entry.getVisibility());

      copy.setValue(entry.getValue().copy());
    }
  }

  // XXX: need to check the other copy, e.g. for sessions

  /**
   * Serializes the value.
   *
   * @param sb holds result of serialization
   * @param serializeMap holds reference indexes
   */
  @Override
  public void serialize(Env env,
                        StringBuilder sb,
                        SerializeMap serializeMap)
  {
    Integer index = serializeMap.get(this);

    if (index != null) {
      sb.append("r:");
      sb.append(index);
      sb.append(";");

      return;
    }

    serializeMap.put(this);
    serializeMap.incrementIndex();

    sb.append("O:");
    sb.append(_className.length());
    sb.append(":\"");
    sb.append(_className);
    sb.append("\":");
    sb.append(getSize());
    sb.append(":{");

    Iterator<Entry> iter = _fieldMap.values().iterator();

    while (iter.hasNext()) {
      Entry entry = iter.next();

      sb.append("s:");

      Value key = entry.getKey();
      int len = key.length();

      if (entry._visibility == FieldVisibility.PROTECTED) {
        sb.append(len + 3);

        sb.append(":\"");
        sb.append("\u0000*\u0000");
      }
      else if (entry._visibility == FieldVisibility.PRIVATE) {
        sb.append(len + 3);

        sb.append(":\"");
        sb.append("\u0000A\u0000");
      }
      else {
        sb.append(len);

        sb.append(":\"");
      }

      sb.append(key);
      sb.append("\";");

      Value value = ((Entry) entry).getRawValue();

      value.serialize(env, sb, serializeMap);
    }

    sb.append("}");
  }

  /**
   * Exports the value.
   */
  @Override
  protected void varExportImpl(StringValue sb, int level)
  {
    if (level != 0) {
      sb.append('\n');
    }

    for (int i = 0; i < level; i++) {
      sb.append("  ");
    }

    sb.append(getName());
    sb.append("::__set_state(array(\n");

    for (Map.Entry<Value,Value> entry : entrySet()) {
      Value key = entry.getKey();
      Value value = entry.getValue();

      for (int i = 0; i < level; i++) {
        sb.append("  ");
      }

      sb.append("   ");

      key.varExportImpl(sb, level + 1);

      sb.append(" => ");

      value.varExportImpl(sb, level + 1);
      sb.append(",\n");
    }

    for (int i = 0; i < level; i++) {
      sb.append("  ");
    }

    sb.append("))");
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder(Env env)
  {
    return toString(env).toStringBuilder(env);
  }

  /**
   * Converts to a java String object.
   */
  public String toJavaString()
  {
    return toString(Env.getInstance()).toString();
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public StringValue toString(Env env)
  {
    AbstractFunction toString = _quercusClass.getToString();

    if (toString != null)
      return toString.callMethod(env, _quercusClass, this).toStringValue();
    else
      return env.createString(_className + "[]");
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public void print(Env env)
  {
    env.print(toString(env));
  }

  /**
   * Converts to an array.
   */
  @Override
  public ArrayValue toArray()
  {
    ArrayValue array = new ArrayValueImpl();

    for (Map.Entry<Value,Value> entry : entrySet()) {
      array.put(entry.getKey(), entry.getValue());
    }

    return array;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Value toObject(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return this;
  }

  @Override
  public Set<? extends Map.Entry<Value,Value>> entrySet()
  {
    return new EntrySet();
  }

  //
  // debugging
  //

  //XXX: push up to super, and use varDumpObject
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int size = getSize();

    if (isIncompleteObject())
      size++;

    out.println("object(" + getName() + ") (" + size + ") {");

    if (isIncompleteObject()) {
      printDepth(out, 2 * (depth + 1));
      out.println("[\"__Quercus_Incomplete_Class_name\"]=>");

      printDepth(out, 2 * (depth + 1));

      Value value = env.createString(getIncompleteObjectName());

      value.varDump(env, out, depth + 1, valueSet);

      out.println();
    }

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

      entry.varDumpImpl(env, out, depth + 1, valueSet);
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.print(getName());
    out.print(' ');
    out.println("Object");
    printDepth(out, 4 * depth);
    out.println("(");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ObjectExtValue.Entry entry = (ObjectExtValue.Entry) mapEntry;

      entry.printRImpl(env, out, depth + 1, valueSet);
    }

    printDepth(out, 4 * depth);
    out.println(")");
  }

  //
  // Java Serialization
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(_className);

    out.writeInt(_fieldMap.size());

    for (Map.Entry<Value,Value> entry : entrySet()) {
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  /**
   * Encodes the value in JSON.
   */
  @Override
  public void jsonEncode(Env env, JsonEncodeContext context, StringValue sb)
  {
    sb.append('{');

    int length = 0;

    Iterator<Entry> iter = _fieldMap.values().iterator();

    while (iter.hasNext()) {
      Entry entry = iter.next();

      if (entry.getVisibility() != FieldVisibility.PUBLIC)
        continue;

      if (length > 0)
        sb.append(',');

      entry.getKey().toStringValue(env).jsonEncode(env, context, sb);
      sb.append(':');
      entry.getValue().jsonEncode(env, context, sb);
      length++;
    }

    sb.append('}');
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    Env env = Env.getInstance();
    String name = (String) in.readObject();

    QuercusClass cl = env.findClass(name);

    init();

    if (cl != null) {
      setQuercusClass(cl);
    }
    else {
      cl = env.getQuercus().getStdClass();

      setQuercusClass(cl);

      setIncompleteObjectName(name);
    }

    int size = in.readInt();

    for (int i = 0; i < size; i++) {
      putThisField(env,
                   (StringValue) in.readObject(),
                   (Value) in.readObject());
    }
  }

  @Override
  public boolean issetField(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    if (entry != null && entry.isPublic()) {
      return entry._value.isset();
    }

    boolean result = getQuercusClass().issetField(env, this, name);

    return result;
  }

  @Override
  public boolean isFieldExists(Env env, StringValue name)
  {
    Entry entry = getThisEntry(name);

    return entry != null;
  }

  @Override
  public String toString()
  {
    if (CurrentTime.isTest())
      return getClass().getSimpleName() +  "[" + _className + "]";
    else
      return getClass().getSimpleName()
             + "@" + System.identityHashCode(this)
             + "[" + _className + "]";
  }

  public class EntrySet extends AbstractSet<Map.Entry<Value,Value>> {
    EntrySet()
    {
    }

    @Override
    public int size()
    {
      return ObjectExtValue.this.getSize();
    }

    @Override
    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new KeyValueIterator(_fieldMap.values().iterator());
    }
  }

  public static class KeyValueIterator
    implements Iterator<Map.Entry<Value,Value>>
  {
    private final Iterator<Entry> _iter;

    KeyValueIterator(Iterator<Entry> iter)
    {
      _iter = iter;
    }

    public boolean hasNext()
    {
      return _iter.hasNext();
    }

    public Map.Entry<Value,Value> next()
    {
      return _iter.next();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class ValueIterator
    implements Iterator<Value>
  {
    private final Iterator<Entry> _iter;

    ValueIterator(Iterator<Entry> iter)
    {
      _iter = iter;
    }

    public boolean hasNext()
    {
      return _iter.hasNext();
    }

    public Value next()
    {
      return _iter.next().getValue();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class KeyIterator
    implements Iterator<Value>
  {
    private final Iterator<StringValue> _iter;

    KeyIterator(Iterator<StringValue> iter)
    {
      _iter = iter;
    }

    public boolean hasNext()
    {
      return _iter.hasNext();
    }

    public Value next()
    {
      return _iter.next();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public final static class Entry
    implements Map.Entry<Value,Value>,
               Comparable<Map.Entry<Value, Value>>
  {
    private final StringValue _key;
    private final FieldVisibility _visibility;
    private Value _value;

    private Entry _next;

    public Entry(StringValue key)
    {
      _key = key;
      _visibility = FieldVisibility.PUBLIC;
      _value = NullValue.NULL;
    }

    public Entry(StringValue key, FieldVisibility visibility)
    {
      _key = key;
      _visibility = visibility;
      _value = NullValue.NULL;
    }

    public Entry(StringValue key, Value value)
    {
      _key = key;
      _visibility = FieldVisibility.PUBLIC;
      _value = value;
    }

    public Entry(StringValue key, Value value, FieldVisibility visibility)
    {
      _key = key;
      _visibility = visibility;
      _value = value;
    }

    public Entry(Env env, IdentityHashMap<Value,Value> map, Entry entry)
    {
      _key = entry._key;
      _visibility = entry._visibility;
      _value = entry._value.copy(env, map);
    }

    public Value getValue()
    {
      return _value.toValue();
    }

    public Value getRawValue()
    {
      return _value;
    }

    public StringValue getKey()
    {
      return _key;
    }

    public FieldVisibility getVisibility()
    {
      return _visibility;
    }

    public Entry getNext()
    {
      return _next;
    }

    public void setNext(Entry next)
    {
      _next = next;
    }

    public boolean isPublic()
    {
      return _visibility == FieldVisibility.PUBLIC;
    }

    public boolean isPrivate()
    {
      return _visibility == FieldVisibility.PRIVATE;
    }

    public Value toValue()
    {
      // The value may be a var
      // XXX: need test
      return _value.toValue();
    }

    /**
     * Argument used/declared as a ref.
     */
    public Var toRefVar()
    {
      Var var = _value.toLocalVarDeclAsRef();

      _value = var;

      return var;
    }

    /**
     * Converts to an argument value.
     */
    public Value toArgValue()
    {
      return _value.toValue();
    }

    public Value setValue(Value value)
    {
      Value oldValue = toValue();

      _value = value;

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function arguments)
     */
    public Value toRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new ArgRef((Var) value);
      else {
        Var var = new Var(_value);

        _value = var;

        return new ArgRef(var);
      }
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new ArgRef((Var) value);
      else {
        Var var = new Var(_value);

        _value = var;

        return new ArgRef(var);
      }
    }

    public Value toArg()
    {
      Value value = _value;

      if (value instanceof Var)
        return value;
      else {
        Var var = new Var(_value);

        _value = var;

        return var;
      }
    }

    Entry copyTree(Env env, CopyRoot root)
    {
      Value copy = root.getCopy(_value);

      if (copy == null)
        copy = _value.copyTree(env, root);

      return new Entry(_key, copy, _visibility);
    }

    public int compareTo(Map.Entry<Value, Value> other)
    {
      if (other == null)
        return 1;

      Value thisKey = getKey();
      Value otherKey = other.getKey();

      if (thisKey == null)
        return otherKey == null ? 0 : -1;

      if (otherKey == null)
        return 1;

      return thisKey.cmp(otherKey);
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      String suffix = "";

      if (_visibility == FieldVisibility.PROTECTED)
        suffix = ":protected";
      else if (_visibility == FieldVisibility.PRIVATE)
        suffix = ":private";

      printDepth(out, 2 * depth);
      out.println("[\"" + getKey() + suffix + "\"]=>");

      printDepth(out, 2 * depth);

      _value.varDump(env, out, depth, valueSet);

      out.println();
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      String suffix = "";

      if (_visibility == FieldVisibility.PROTECTED)
        suffix = ":protected";
      else if (_visibility == FieldVisibility.PRIVATE)
        suffix = ":private";

      printDepth(out, 4 * depth);
      out.print("[" + getKey() + suffix + "] => ");

      _value.printR(env, out, depth + 1, valueSet);

      out.println();
    }

    private void printDepth(WriteStream out, int depth)
      throws java.io.IOException
    {
      for (int i = 0; i < depth; i++)
        out.print(' ');
    }

    @Override
    public String toString()
    {
      return "ObjectExtValue.Entry[" + getKey() + "]";
    }
  }
}


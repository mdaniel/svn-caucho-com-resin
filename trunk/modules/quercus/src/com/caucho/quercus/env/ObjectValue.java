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

import com.caucho.quercus.expr.LiteralNullExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.lib.ArrayModule;
import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.ClassField;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a Quercus object value.
 */
abstract public class ObjectValue extends Callback {
  transient protected QuercusClass _quercusClass;

  protected String _className;
  protected String _incompleteObjectName;

  private final int _objectId;

  protected ObjectValue(Env env)
  {
    _objectId = env.generateObjectId();
  }

  protected ObjectValue(Env env, QuercusClass quercusClass)
  {
    this(env);

    _quercusClass = quercusClass;
    _className = quercusClass.getName();
  }

  public void setQuercusClass(QuercusClass cl)
  {
    _quercusClass = cl;
    _className = cl.getName();
  }

  @Override
  public QuercusClass getQuercusClass()
  {
    return _quercusClass;
  }

  @Override
  public QuercusClass findQuercusClass(Env env)
  {
    return _quercusClass;
  }

  public AbstractFunction getMethod(StringValue name)
  {
    return getQuercusClass().getMethod(name);
  }

  public boolean isIncompleteObject()
  {
    return _incompleteObjectName != null;
  }

  /**
   * Returns the name of the uninitialized object.
   */
  public String getIncompleteObjectName()
  {
    return _incompleteObjectName;
  }

  /**
   * Sets the name of uninitialized object.
   */
  public void setIncompleteObjectName(String name)
  {
    _incompleteObjectName = name;
  }

  /**
   * Initializes the incomplete class.
   */
  public void initObject(Env env, QuercusClass cls)
  {
    setQuercusClass(cls);
    _incompleteObjectName = null;
  }

  public final void cleanup(Env env)
  {
    QuercusClass qClass = getQuercusClass();
    AbstractFunction fun = qClass.getDestructor();

    if (fun != null) {
      fun.callMethod(env, qClass, this);
    }
  }

  /**
   * Returns the value's class name.
   */
  public String getClassName()
  {
    return _className;
  }

  public void setClassName(String className)
  {
    _className = className;
  }

  /**
   * Returns a Set of entries.
   */
  // XXX: remove entrySet() and use getIterator() instead
  abstract public Set<? extends Map.Entry<Value,Value>> entrySet();

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _className;
  }

  /**
   * Returns the parent class
   */
  public String getParentClassName()
  {
    return _quercusClass.getParentName();
  }

  /**
   * Returns true for an object.
   */
  @Override
  public boolean isObject()
  {
    return true;
  }

  /**
   * Returns the type.
   */
  @Override
  public String getType()
  {
    return "object";
  }

  /**
   * Returns the unique object hash.
   */
  @Override
  public StringValue getObjectHash(Env env)
  {
    StringValue sb = env.createStringBuilder();

    sb.append(getClassName());
    sb.append('-');
    sb.append(_objectId);

    if (! CurrentTime.isTest()) {
      sb.append('-');
      sb.append(System.identityHashCode(this));
    }

    return sb;
  }

  /**
   * The object is callable if it has an __invoke method
   */
  @Override
  public boolean isCallable(Env env, boolean isCheckSyntaxOnly, Value nameRef)
  {
    // php/127c, isCheckSyntaxOnly is not used

    if (_quercusClass.getInvoke() == null) {
      if (nameRef != null) {
        nameRef.set(NullValue.NULL);
      }

      return false;
    }

    if (nameRef != null) {
      StringValue sb = env.createStringBuilder();
      sb.append(_quercusClass.getName());
      sb.append("::");
      sb.append("__invoke");

      nameRef.set(sb);
    }

    return true;
  }

  @Override
  public Callable toCallable(Env env, boolean isOptional)
  {
    if (_quercusClass.getInvoke() != null) {
      return this;
    }
    else {
      return super.toCallable(env, isOptional);
    }
  }

  @Override
  public boolean isValid(Env env)
  {
    return _quercusClass.getInvoke() != null;
  }

  @Override
  public String getCallbackName()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDeclFileName(Env env)
  {
    return null;
  }

  @Override
  public int getDeclStartLine(Env env)
  {
    return -1;
  }

  @Override
  public int getDeclEndLine(Env env)
  {
    return -1;
  }

  @Override
  public String getDeclComment(Env env)
  {
    return null;
  }

  @Override
  public boolean isReturnsReference(Env env)
  {
    return false;
  }

  @Override
  public Arg []getArgs(Env env)
  {
    AbstractFunction fun = _quercusClass.getInvoke();

    if (fun == null) {
      return null;
    }

    return fun.getArgs(env);
  }

  @Override
  public boolean isInternal(Env env)
  {
    return false;
  }

  /**
   * Converts to a boolean.
   */
  @Override
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Returns true for an implementation of a class
   */
  @Override
  public boolean isA(Env env, String name)
  {
    return _quercusClass.isA(env, name);
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return 1;
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toLong();
  }

  /**
   * Converts to a Java Enum.
   */
  @Override
  public Enum toJavaEnum(Env env, Class cls)
  {
    Object obj = toJavaObject();

    if (cls.isAssignableFrom(obj.getClass())) {
      return (Enum) obj;
    }
    else {
      return super.toJavaEnum(env, cls);
    }
  }

  //
  // array delegate methods
  //

  @Override
  public Value toAutoArray()
  {
    return this;
  }

  /**
   * Append to a string builder.
   */
  @Override
  public StringValue appendTo(UnicodeBuilderValue sb)
  {
    return sb.append(toString(Env.getInstance()));
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public StringValue appendTo(StringBuilderValue sb)
  {
    return sb.append(toString(Env.getInstance()));
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public StringValue appendTo(BinaryBuilderValue sb)
  {
    return sb.appendBytes(toString(Env.getInstance()));
  }

  /**
   * Append to a binary builder.
   */
  @Override
  public StringValue appendTo(LargeStringBuilderValue sb)
  {
    return sb.append(toString(Env.getInstance()));
  }

  /**
   * Returns the array value with the given key.
   */
  @Override
  public Value get(Value key)
  {
    Env env = Env.getInstance();

    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    if (delegate != null) {
      return delegate.get(env, this, key);
    }
    else {
      // php/3d94

      // return getField(Env.getInstance(), key.toStringValue());
      return env.error(L.l("Can't use object '{0}' as array", getName()));
    }
  }

  /**
   * Sets the array value with the given key.
   */
  @Override
  public Value put(Value key, Value value)
  {
    Env env = Env.getInstance();

    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    // php/0d94

    if (delegate != null) {
      return delegate.put(env, this, key, value);
    }
    else {
      // php/0d94

      return env.error(L.l("Can't use object '{0}' as array", getName()));
      // return super.put(key, value);
    }
  }

  /**
   * Appends a new array value
   */
  @Override
  public Value put(Value value)
  {
    Env env = Env.getInstance();

    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    // php/0d94

    if (delegate != null)
      return delegate.put(env, this, value);
    else {
      // php/0d97

      return env.error(L.l("Can't use object '{0}' as array", getName()));
      // return super.put(key, value);
    }
  }

  /**
   * Sets the array value, returning the new array, e.g. to handle
   * string update ($a[0] = 'A').  Creates an array automatically if
   * necessary.
   */
  public Value append(Value index, Value value)
  {
    put(index, value);

    return this;
  }

  /**
   * Return true if set
   */
  @Override
  public boolean isset(Value key)
  {
    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    if (delegate != null) {
      Env env = Env.getInstance();

      return delegate.isset(env, this, key);
    }
    else {
      return false;
    }
  }

  /**
   * Return true if empty.
   */
  @Override
  public boolean isEmpty(Env env, Value key)
  {
    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    if (delegate != null) {
      return delegate.isEmpty(env, this, key);
    }
    else {
      return true;
    }
  }

  /**
   * Unsets the array value
   */
  @Override
  public Value remove(Value key)
  {
    ArrayDelegate delegate = _quercusClass.getArrayDelegate();

    if (delegate != null) {
      Env env = Env.getInstance();

      return delegate.unset(env, this, key);
    }
    else
      return super.remove(key);
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
   * Returns an iterator for the keys.
   */
  @Override
  public Iterator<Value> getKeyIterator(Env env)
  {
    TraversableDelegate delegate = _quercusClass.getTraversableDelegate();

    if (delegate != null)
      return delegate.getKeyIterator(env, this);
    else
      return super.getKeyIterator(env);
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
    else
      return super.getValueIterator(env);
  }

  //
  // count delegate methods
  //

  /**
   * Returns the count value with the given key.
   */
  @Override
  public int getCount(Env env)
  {
    CountDelegate delegate = _quercusClass.getCountDelegate();

    // php/066q vs. php/0906
    //return getField(null, key.toString());

    if (delegate != null)
      return delegate.count(env, this);
    else
      return super.getSize();
  }

  //
  // Convenience field methods
  //

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, String value)
  {
    Env env = Env.getInstance();

    return putThisField(env, env.createString(key), env.createString(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, String value)
  {
    return putThisField(env, env.createString(key), env.createString(value));
  }

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, long value)
  {
    Env env = Env.getInstance();

    return putThisField(env, env.createString(key), LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, long value)
  {
    return putThisField(env, env.createString(key), LongValue.create(value));
  }

  /**
   * Adds a new value.
   */
  public Value putField(Env env, String key, Value value)
  {
    return putThisField(env, env.createString(key), value);
  }

  /**
   * Initializes a new field, does not call __set if it is defined.
   */
  @Override
  public void initField(Env env,
                        StringValue name,
                        StringValue canonicalName,
                        Value value)
  {
    putThisField(env, canonicalName, value);
  }

  @Override
  public void initIncompleteField(Env env,
                                  StringValue name,
                                  Value value,
                                  FieldVisibility visibility)
  {
    initField(env, name, value);
  }

  /**
   * Adds a new value.
   * @Deprecated
   */
  public Value putField(String key, double value)
  {
    Env env = Env.getInstance();

    return putThisField(env, env.createString(key), DoubleValue.create(value));
  }

  /**
   * Returns the static field.
   */
  @Override
  public Value getStaticFieldValue(Env env, StringValue name)
  {
    return getQuercusClass().getStaticFieldValue(env, name);
  }

  /**
  * Returns the static field reference.
  */
  @Override
  public Var getStaticFieldVar(Env env, StringValue name)
  {
    return getQuercusClass().getStaticFieldVar(env, name);
  }

  /**
   * Sets the static field.
   */
  @Override
  public Value setStaticFieldRef(Env env, StringValue name, Value value)
  {
    return getQuercusClass().setStaticFieldRef(env, name, value);
  }

  /**
   * Returns true for equality
   */
  @Override
  public boolean eq(Value rValue)
  {
    if (rValue.isObject()) {
      rValue = rValue.toValue();

      return cmpObject((ObjectValue) rValue) == 0;
    }
    else if (rValue.isArray()) {
      return false;
    }
    else {
      // php/03q0
      return rValue.eq(this.toStringValue());
    }
  }

  /**
   * Compare two objects
   */
  public int cmpObject(ObjectValue rValue)
  {
    if (rValue == this)
      return 0;

    // if objects are not equal, then which object is greater is undefined

    int result = getName().compareTo(rValue.getName());

    if (result != 0)
      return result;

    Set<? extends Map.Entry<Value,Value>> aSet = entrySet();
    Set<? extends Map.Entry<Value,Value>> bSet = rValue.entrySet();

    if (aSet.equals(bSet))
      return 0;
    else if (aSet.size() > bSet.size())
      return 1;
    else if (aSet.size() < bSet.size())
      return -1;
    else {
      TreeSet<Map.Entry<Value,Value>> aTree
        = new TreeSet<Map.Entry<Value,Value>>(aSet);

      TreeSet<Map.Entry<Value,Value>> bTree
        = new TreeSet<Map.Entry<Value,Value>>(bSet);

      Iterator<Map.Entry<Value,Value>> iterA = aTree.iterator();
      Iterator<Map.Entry<Value,Value>> iterB = bTree.iterator();

      while (iterA.hasNext()) {
        Map.Entry<Value,Value> a = iterA.next();
        Map.Entry<Value,Value> b = iterB.next();

        result = a.getKey().cmp(b.getKey());

        if (result != 0)
          return result;

        result = a.getValue().cmp(b.getValue());

        if (result != 0)
          return result;
      }

      // should never reach this
      return 0;
    }
  }

  /**
   * Finds the method name.
   */
  @Override
  public final AbstractFunction findFunction(StringValue methodName)
  {
    return _quercusClass.findFunction(methodName);
  }

  /**
   * Call for callable.
   */
  @Override
  public Value call(Env env, Value []args)
  {
    AbstractFunction fun = _quercusClass.getInvoke();

    if (fun != null)
      return fun.callMethod(env, _quercusClass, this, args);
    else {
      return env.warning(L.l("{0} is not a valid function",
                             this));
    }
  }

  public void varDumpObject(Env env,
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

    ArrayValue sortedEntries = new ArrayValueImpl();

    Iterator<Map.Entry<Value,Value>> iter = getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      sortedEntries.put(entry.getKey(), entry.getValue());
    }

    ArrayModule.ksort(env, sortedEntries, ArrayModule.SORT_STRING);

    iter = sortedEntries.getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();

      Value key = entry.getKey();
      Value value = entry.getValue();

      printDepth(out, 2 * depth);
      out.println("[\"" + key + "\"]=>");

      depth++;

      printDepth(out, 2 * depth);

      value.varDump(env, out, depth, valueSet);

      out.println();

      depth--;
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  /**
   * Encodes the value in JSON.
   */
  @Override
  public void jsonEncode(Env env, JsonEncodeContext context, StringValue sb)
  {
    if (isA(env, "JsonSerializable")) {
      AbstractFunction fun = getMethod(env.createString("jsonSerialize"));

      if (fun == null) {
        throw new IllegalStateException(L.l("must implement jsonSerialize()"));
      }

      Value value = fun.callMethod(env, getQuercusClass(), this);

      value.jsonEncode(env, context, sb);

      return;
    }
    else {
      sb.append('{');

      int length = 0;

      Iterator<Map.Entry<Value,Value>> iter = getIterator(env);

      while (iter.hasNext()) {
        Map.Entry<Value,Value> entry = iter.next();

        StringValue key = entry.getKey().toStringValue(env);
        Value value = entry.getValue();

        if (! ClassField.isPublic(key)) {
          continue;
        }

        if (length > 0) {
          sb.append(',');
        }

        key.jsonEncode(env, context, sb);
        sb.append(':');
        value.jsonEncode(env, context, sb);
        length++;
      }

      sb.append('}');
    }
  }
}


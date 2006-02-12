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

package com.caucho.quercus.env;

import com.caucho.vfs.WriteStream;

import java.util.*;
import java.util.logging.Logger;

/**
 * Represents a PHP array value.
 */
abstract public class ArrayValue extends Value {
  private static final Logger log
    = Logger.getLogger(ArrayValue.class.getName());

  protected static final StringValue KEY = new StringValue("key");
  protected static final StringValue VALUE = new StringValue("value");

  private static final int SORT_REGULAR = 0;
  private static final int SORT_NUMERIC = 1;
  private static final int SORT_STRING = 2;
  private static final int SORT_LOCALE_STRING = 5;

  public static final GetKey GET_KEY = new GetKey();
  public static final GetValue GET_VALUE = new GetValue();

  protected Entry _current;

  protected ArrayValue()
  {
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "array";
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return getSize() != 0;
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    return "Array";
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Returns true for an array.
   */
  public boolean isArray()
  {
    return true;
  }

  /**
   * Copy as a return value
   */
  public Value copyReturn()
  {
    return copy(); // php/3a5e
  }

  /**
   * Copy for assignment.
   */
  abstract public Value copy();

  /**
   * Copy for serialization
   */
  abstract public Value copy(Env env, IdentityHashMap<Value,Value> map);

  /**
   * Returns the size.
   */
  abstract public int getSize();

  /**
   * Clears the array
   */
  abstract public void clear();

  /**
   * Adds a new value.
   */
  abstract public Value put(Value key, Value value);

  /**
   * Add
   */
  abstract public ArrayValue put(Value value);

  /**
   * Add to front.
   */
  abstract public ArrayValue unshift(Value value);

  /**
   * Returns the value as an array.
   */
  public Value getArray(Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = new ArrayValueImpl();

      put(fieldName, value);
    }

    return value;
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  abstract public Value getArg(Value index);

  /**
   * Returns the field value, creating an object if it's unset.
   */
  public Value getObject(Env env, Value fieldName)
  {
    Value value = get(fieldName);

    if (! value.isset()) {
      value = env.createObject();

      put(fieldName, value);
    }

    return value;
  }

  /**
   * Sets the array ref.
   */
  abstract public Value putRef();

  /**
   * Creatse a tail index.
   */
  abstract public Value createTailKey();

  /**
   * Adds to the following value.
   */
  public Value add(Value rValue)
    throws Throwable
  {
    rValue = rValue.toValue();

    if (! (rValue instanceof ArrayValue))
      return copy();

    ArrayValue rArray = (ArrayValue) rValue;

    ArrayValue result = new ArrayValueImpl(rArray);

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      result.put(entry.getKey(), entry.getValue());
    }

    return result;
  }

  /**
   * Returns the field values.
   */
  public Collection<Value> getIndices()
  {
    return new KeySet();
  }

  /**
   * Returns the field keys.
   */
  public Value []getKeyArray()
  {
    int size = getSize();

    if (size == 0)
      return NULL_VALUE_ARRAY;

    Value []keys = new Value[size];

    int i = 0;
    for (Entry ptr = getHead(); ptr != null; ptr = ptr._next)
      keys[i++] = ptr.getKey();

    return keys;
  }

  /**
   * Returns the field values.
   */
  public Value []getValueArray(Env env)
  {
    int size = getSize();

    if (size == 0)
      return NULL_VALUE_ARRAY;

    Value []values = new Value[size];

    int i = 0;
    for (Entry ptr = getHead(); ptr != null; ptr = ptr._next) {
      // XXX: the toValue() needs a test
      values[i++] = ptr.getValue().toValue().copy();
    }

    return values;
  }

  /**
   * Gets a new value.
   */
  abstract public Value get(Value key);

  /**
   * Removes a value.
   */
  abstract public Value remove(Value key);

  /**
   * Returns the array ref.
   */
  abstract public Var getRef(Value index);

  /**
   * Returns an iterator of the entries.
   */
  public Set<Value> keySet()
  {
    return new KeySet();
  }

  /**
   * Returns a set of all the of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    return new EntrySet();
  }

  /**
   * Returns a collection of the values.
   */
  public Collection<Value> values()
  {
    return new ValueCollection();
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, String value)
  {
    put(new StringValue(key), new StringValue(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, long value)
  {
    put(new StringValue(key), new LongValue(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, boolean value)
  {
    put(new StringValue(key), value ? BooleanValue.TRUE : BooleanValue.FALSE);
  }

  /**
   * Convenience for lib.
   */
  public void put(String value)
  {
    put(new StringValue(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(long value)
  {
    put(new LongValue(value));
  }

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  public ArrayValue append(Value key, Value value)
  {
    put(key, value);

    return this;
  }

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  public ArrayValue append(Value value)
  {
    put(value);

    return this;
  }

  /**
   * Appends as an argument - only called from compiled code
   *
   * XXX: change name to appendArg
   */
  public void putAll(ArrayValue array)
  {
    for (Map.Entry<Value, Value> entry : array.entrySet())
      put(entry.getKey(), entry.getValue());
  }

  /**
   * Convert to an array.
   */
  public static Value toArray(Value value)
  {
    value = value.toValue();

    if (value instanceof ArrayValue)
      return value;
    else
      return new ArrayValueImpl().put(value);
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws Throwable
  {
    env.getOut().print("Array");
  }

  /**
   * Pops the top value.
   */
  abstract public Value pop();

  /**
   * Shuffles the array
   */
  abstract public void shuffle();

  /**
   * Returns the head.
   */
  abstract protected Entry getHead();

  /**
   * Returns the tail.
   */
  abstract protected Entry getTail();

  /**
   * Returns the current value.
   */
  public Value current()
  {
    if (_current != null)
      return _current.getValue();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the current key
   */
  public Value key()
  {
    if (_current != null)
      return _current.getKey();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns true if there are more elements.
   */
  public boolean hasCurrent()
  {
    return _current != null;
  }

  /**
   * Returns the next value.
   */
  public Value next()
  {
    if (_current != null)
      _current = _current._next;

    return current();
  }

  /**
   * Returns the previous value.
   */
  public Value prev()
  {
    if (_current != null)
      _current = _current._prev;

    return current();
  }

  /**
   * The each iterator
   */
  public Value each()
  {
    if (_current == null)
      return BooleanValue.FALSE;

    ArrayValue result = new ArrayValueImpl();

    result.put(LongValue.ZERO, _current.getKey());
    result.put(KEY, _current.getKey());

    result.put(LongValue.ONE, _current.getValue());
    result.put(VALUE, _current.getValue());

    _current = _current._next;

    return result;
  }

  /**
   * Returns the first value.
   */
  public Value reset()
  {
    _current = getHead();

    return current();
  }

  /**
   * Returns the last value.
   */
  public Value end()
  {
    _current = getTail();

    return current();
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value  the value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  public Value contains(Value value)
  {
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      if (entry.getValue().eq(value))
        return entry.getKey();
    }

    return NullValue.NULL;
  }

  /**
   * Returns the corresponding key if this array contains the given value
   *
   * @param value  the value to search for in the array
   *
   * @return the key if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  public Value containsStrict(Value value)
    throws Throwable
  {
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      if (entry.getValue().eql(value))
        return entry.getKey();
    }

    return NullValue.NULL;
  }

  /**
   * Returns the corresponding valeu if this array contains the given key
   *
   * @param key  the key to search for in the array
   *
   * @return the value if it is found in the array, NULL otherwise
   *
   * @throws NullPointerException
   */
  abstract public Value containsKey(Value key);

  /**
   * Returns an object array of this array.  This is a copy of this object's
   * backing structure.  Null elements are not included.
   *
   * @return an object array of this array
   */
  public Map.Entry<Value, Value>[] toArray()
  {
    ArrayList<Map.Entry<Value, Value>> array =
      new ArrayList<Map.Entry<Value, Value>>(getSize());

    for (Entry entry = getHead(); entry != null; entry = entry._next)
      array.add(entry);

    Map.Entry<Value, Value>[]result = new Entry[array.size()];

    return array.toArray(result);
  }

  /**
   * Sorts this array based using the passed Comparator
   *
   * @param comparator the comparator for sorting the array
   * @param resetKeys  true if the keys should not be preserved
   * @param strict  true if alphabetic keys should not be preserved
   */
  public void sort(Comparator<Map.Entry<Value, Value>> comparator,
                   boolean resetKeys, boolean strict)
    throws Throwable
  {
    Entry []entries;

    entries = new Entry[getSize()];

    int i = 0;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entries[i++] = entry;
    }

    Arrays.sort(entries, comparator);

    clear();

    long base = 0;

    if (! resetKeys)
      strict = false;

    for (int j = 0; j < entries.length; j++) {
      Value key = entries[j].getKey();

      if (resetKeys && (! (key instanceof StringValue) || strict))
        put(LongValue.create(base++), entries[j].getValue());
      else
        put(entries[j].getKey(), entries[j].getValue());
    }
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    sb.append("a:");
    sb.append(getSize());
    sb.append(":{");

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entry.getKey().serialize(sb);
      entry.getValue().serialize(sb);
    }

    sb.append("}");
  }

  /**
   * Exports the value.
   */
  public void varExport(StringBuilder sb)
  {
    sb.append("array(");

    boolean isFirst = true;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entry.getKey().varExport(sb);
      sb.append(" => ");
      entry.getValue().varExport(sb);
      sb.append(", ");
    }

    sb.append(")");
  }

  /**
   * Resets all numerical keys with the first index as base
   *
   * @param base  the initial index
   * @param strict  if true, string keys are also reset
   */
  public boolean keyReset(long base, boolean strict)
  {
    Entry []entries;

    entries = new Entry[getSize()];

    int i = 0;
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      entries[i++] = entry;
    }

    clear();

    for (int j = 0; j < entries.length; j++) {
      Value key = entries[j].getKey();

      if (! (key instanceof StringValue) || strict)
        put(LongValue.create(base++), entries[j].getValue());
      else
        put(entries[j].getKey(), entries[j].getValue());
    }

    return true;
  }

  /**
   * Test for equality
   *
   * @param rValue rhs ArrayValue to compare to
   *
   * @return true if this is equal to rValue, false otherwise
   */
  public boolean eq(Value rValue)
  {
    if (rValue == null)
      return false;

    for (Map.Entry<Value, Value> entry: entrySet()) {
      Value entryValue = entry.getValue();

      Value entryKey = entry.getKey();

      Value rEntryValue = rValue.get(entryKey);

      if ((rEntryValue instanceof ArrayValue) &&
          !entryValue.eq((ArrayValue) rEntryValue))
        return false;

      if (! entryValue.eq(rEntryValue))
        return false;
    }

    return true;
  }

  /**
   * Test for ===
   *
   * @param rValue rhs ArrayValue to compare to
   *
   * @return true if this is equal to rValue, false otherwise
   */
  public boolean eql(Value rValue)
    throws Throwable
  {
    if (rValue == null)
      return false;
    else if (getSize() != rValue.getSize())
      return false;

    rValue = rValue.toValue();

    if (! (rValue instanceof ArrayValue))
      return false;

    ArrayValue rArray = (ArrayValue) rValue;

    Iterator<Map.Entry<Value,Value>> iterA = entrySet().iterator();
    Iterator<Map.Entry<Value,Value>> iterB = rArray.entrySet().iterator();

    while (iterA.hasNext() && iterB.hasNext()) {
      Map.Entry<Value,Value> entryA = iterA.next();
      Map.Entry<Value,Value> entryB = iterB.next();

      if (! entryA.getKey().eql(entryB.getKey()))
        return false;

      if (! entryA.getValue().eql(entryB.getValue()))
        return false;
    }

    if (iterA.hasNext() || iterB.hasNext())
      return false;
    else
      return true;
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    out.println("array(" + getSize() + ") {");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

      entry.varDump(env, out, depth + 1, valueSet);

      out.println();
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws Throwable
  {
    out.println("Array");
    printDepth(out, 8 * depth);
    out.println("(");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

      entry.printR(env, out, depth, valueSet);
    }

    printDepth(out, 8 * depth);
    out.println(")");
  }

  public final static class Entry extends Var
    implements Map.Entry<Value,Value> {
    Entry _prev;
    Entry _next;

    Value _key;

    public Entry()
    {
    }

    public Entry(Value key, Value value)
    {
      _key = key;
      setValue(value);
    }

    public Entry getNext()
    {
      return _next;
    }

    public Value getValue()
    {
      return getRawValue().toValue();
    }

    public Value getKey()
    {
      return _key;
    }

    public Value toValue()
    {
      // The value may be a var
      // XXX: need test
      return getRawValue().toValue();
    }

    /**
     * Argument used/declared as a ref.
     */
    public Var toRefVar()
    {
      // php/376a

      Value val = getRawValue();

      if (val instanceof Var)
        return (Var) val;
      else {
        Var var = new Var(val);

        setRaw(var);

        return var;
      }
    }

    /**
     * Converts to an argument value.
     */
    public Value toArgValue()
    {
      return getRawValue().toValue();
    }

    public Value setValue(Value value)
    {
      Value oldValue = toValue();

      setRaw(value);

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toRef()
    {
      Value value = toValue();

      if (value instanceof Var)
        return new RefVar((Var) value);
      else
	return new RefVar(this);
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      Value value = toValue();

      if (value instanceof Var)
        return new RefVar((Var) value);
      else
        return new RefVar(this);
    }

    public Value toArg()
    {
      // php/39a4
      Value value = getRawValue();

      // php/39aj
      if (value instanceof Var)
        return value;
      else
        return this;
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws Throwable
    {
      printDepth(out, 2 * depth);
      out.print("[");

      if (_key instanceof StringValue)
        out.print("\"" + _key + "\"");
      else
        out.print(_key);

      out.println("]=>");

      printDepth(out, 2 * depth);

      super.toValue().varDump(env, out, depth, valueSet);
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws Throwable
    {
      printDepth(out, 8 * depth);
      out.print("    [");
      out.print(_key);
      out.print("] => ");
      super.toValue().printR(env, out, depth + 1, valueSet);
      out.println();
    }

    public String toString()
    {
      return "ArrayValue.Entry[" + getKey() + "]";
    }
  }

  /**
   * Takes the values of this array and puts them in a vector
   */
  public Value[] valuesToArray()
  {
    Value[] values = new Value[getSize()];

    int i = 0;
    for (Entry ptr = getHead(); ptr != null; ptr = ptr.getNext()) {
      values[i++] = ptr.getValue();
    }

    return values;
  }

  public class EntrySet extends AbstractSet<Map.Entry<Value,Value>> {
    EntrySet()
    {
    }

    public int size()
    {
      return ArrayValue.this.getSize();
    }

    public Iterator<Map.Entry<Value,Value>> iterator()
    {
      return new EntryIterator(getHead());
    }
  }

  public class KeySet extends AbstractSet<Value> {
    KeySet()
    {
    }

    public int size()
    {
      return ArrayValue.this.getSize();
    }

    public Iterator<Value> iterator()
    {
      return new KeyIterator(getHead());
    }
  }

  public class ValueCollection extends AbstractCollection<Value> {
    ValueCollection()
    {
    }

    public int size()
    {
      return ArrayValue.this.getSize();
    }

    public Iterator<Value> iterator()
    {
      return new ValueIterator(getHead());
    }
  }

  public static class EntryIterator
    implements Iterator<Map.Entry<Value,Value>> {
    private Entry _current;

    EntryIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Map.Entry<Value,Value> next()
    {
      if (_current != null) {
        Map.Entry<Value,Value> next = _current;
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class KeyIterator
    implements Iterator<Value> {
    private Entry _current;

    KeyIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Value next()
    {
      if (_current != null) {
        Value next = _current.getKey();
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class ValueIterator
    implements Iterator<Value> {
    private Entry _current;

    ValueIterator(Entry head)
    {
      _current = head;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Value next()
    {
      if (_current != null) {
        Value next = _current.getValue();
        _current = _current._next;

        return next;
      }
      else
        return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  public static class ValueComparator implements
                                      Comparator<Map.Entry<Value,Value>> {
    public int compare(Map.Entry<Value,Value> aEntry,
                       Map.Entry<Value,Value> bEntry)
    {
      try {
        Value aValue = aEntry.getValue();
        Value bValue = bEntry.getValue();

        if (aValue.eq(bValue))
          return 0;
        else if (aValue.lt(bValue))
          return -1;
        else
          return 1;
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class KeyComparator implements
                                    Comparator<Map.Entry<Value,Value>> {
    public int compare(Map.Entry<Value,Value> aEntry,
                       Map.Entry<Value,Value> bEntry)
    {
      try {
        Value aKey = aEntry.getKey();
        Value bKey = bEntry.getKey();

        if (aKey.eq(bKey))
          return 0;
        else if (aKey.lt(bKey))
          return -1;
        else
          return 1;
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static abstract class AbstractGet {
    public abstract Value get(Map.Entry<Value, Value> entry);
  }

  public static class GetKey extends AbstractGet {
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getKey();
    }
  }

  public static class GetValue extends AbstractGet {
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getValue();
    }
  }
}



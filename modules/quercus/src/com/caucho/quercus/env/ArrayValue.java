/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.function.Marshal;
import com.caucho.quercus.function.MarshalFactory;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP array value.
 */
abstract public class ArrayValue extends Value {
  private static final Logger log
    = Logger.getLogger(ArrayValue.class.getName());

  protected static final StringValue KEY = new StringValueImpl("key");
  protected static final StringValue VALUE = new StringValueImpl("value");

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
   * Converts to a java object.
   */
  @Override
  public Object toJavaObject()
  {
    return this;
  }

  //
  // Conversions
  //
  
  /**
   * Converts to an object.
   */
  public Value toArray()
  {
    return this;
  }
  
  /**
   * Converts to an array value
   */
  @Override
  public ArrayValue toArrayValue(Env env)
  {
    return this;
  }

  /**
   * Converts to an object.
   */
  public Value toObject(Env env)
  {
    Value obj = env.createObject();

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      Value key = entry.getKey();

      if (key instanceof StringValue) {
        // XXX: intern?
        obj.putField(env, key.toString(), entry.getValue());
      }
    }

    return obj;
  }

  /**
   * Converts to a java List object.
   */
  public Collection toJavaCollection(Env env, Class type)
  {
    Collection coll = null;
    
    if (type.isAssignableFrom(HashSet.class)) {
      coll = new HashSet();
    }
    else if (type.isAssignableFrom(TreeSet.class)) {
      coll = new TreeSet();
    }
    else {
      try {
        coll = (Collection) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }
    
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      coll.add(entry.getValue().toJavaObject());
    }

    return coll;
  }
  
  /**
   * Converts to a java List object.
   */
  public List toJavaList(Env env, Class type)
  {
    List list = null;
    
    if (type.isAssignableFrom(ArrayList.class)) {
      list = new ArrayList();
    }
    else if (type.isAssignableFrom(LinkedList.class)) {
      list = new LinkedList();
    }
    else if (type.isAssignableFrom(Vector.class)) {
      list = new Vector();
    }
    else {
      try {
        list = (List) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        env.warning(L.l("Can't assign array to {0}", type.getName()));

        return null;
      }
    }

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      list.add(entry.getValue().toJavaObject());
    }

    return list;
  }
  
  /**
   * Converts to a java object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    Map map = null;
    
    if (type.isAssignableFrom(TreeMap.class)) {
      map = new TreeMap();
    }
    else if (type.isAssignableFrom(LinkedHashMap.class)) {
      map = new LinkedHashMap();
    }
    else {
      try {
        map = (Map) type.newInstance();
      }
      catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
	
        env.warning(L.l("Can't assign array to {0}",
			            type.getName()));

	return null;
      }
    }

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      map.put(entry.getKey().toJavaObject(),
	          entry.getValue().toJavaObject());
    }

    return map;
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
  abstract public Value put(Value value);

  /**
   * Add to front.
   */
  abstract public ArrayValue unshift(Value value);

  /**
   * Splices.
   */
  abstract public ArrayValue splice(int begin, int end, ArrayValue replace);

  /**
   * Returns the value as an array.
   */
  public Value getArray(Value index)
  {
    Value value = get(index);

    Value array = value.toAutoArray();
    
    if (value != array) {
      value = array;

      put(index, value);
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

    Value object = value.toAutoObject(env);
    if (value != object) {
      value = object;

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
   * Returns a union of this array and the rValue as array.
   * If the rValue is not an array, the returned union contains the elements
   * of this array only.
   *
   * To append a value to this ArrayValue use the {@link #put(Value)} method.
   */
  public Value add(Value rValue)
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
  public Value []getKeyArray(Env env)
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
   * Returns the value in the array as-is.
   * (i.e. without calling toValue() on it).
   */
  public Value getRaw(Value key)
  {
    return get(key);
  }
  
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
    put(new StringValueImpl(key), new StringValueImpl(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, char value)
  {
    put(new StringValueImpl(key), StringValue.create(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, long value)
  {
    put(new StringValueImpl(key), new LongValue(value));
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, boolean value)
  {
    put(new StringValueImpl(key), value ? BooleanValue.TRUE : BooleanValue.FALSE);
  }

  /**
   * Convenience for lib.
   */
  public void put(String value)
  {
    put(new StringValueImpl(value));
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
   * Compare two arrays.
   */
  public int compareArray(ArrayValue rValue)
  {
    // XXX: "if key from operand 1 is not found in operand 2 then
    // arrays are uncomparable, otherwise - compare value by value"
    int lArraySize = this.getSize();
    int rArraySize = rValue.getSize();

    if (lArraySize < rArraySize) return -1;
    if (lArraySize > rArraySize) return 1;
    return 0;
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
    env.print("Array");
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
  abstract public Entry getHead();

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
      return NullValue.NULL;
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
      return NullValue.NULL;

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
  public Map.Entry<Value, Value>[] toEntryArray()
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
    throws IOException
  {
    out.println("array(" + getSize() + ") {");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

      entry.varDumpImpl(env, out, depth + 1, valueSet);

      out.println();
    }

    printDepth(out, 2 * depth);

    out.print("}");
  }

  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    out.println("Array");
    printDepth(out, 8 * depth);
    out.println("(");

    for (Map.Entry<Value,Value> mapEntry : entrySet()) {
      ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;

      entry.printRImpl(env, out, depth, valueSet);
    }

    printDepth(out, 8 * depth);
    out.println(")");
  }

  public final static class Entry
    implements Map.Entry<Value,Value>, Serializable  
  {
    final Value _key;
    Value _value;

    Entry _prev;
    Entry _next;

    Entry _prevHash;
    Entry _nextHash;

    int _index;

    public Entry(Value key)
    {
      _key = key;
      _value = NullValue.NULL;
    }

    public Entry(Value key, Value value)
    {
      _key = key;
      _value = value;
    }

    public Entry getNext()
    {
      return _next;
    }

    public Value getRawValue()
    {
      return _value;
    }
    
    public Value getValue()
    {
      return _value.toValue();
    }

    public Value getKey()
    {
      return _key;
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
      // php/376a

      Value val = _value;

      if (val instanceof Var)
        return (Var) val;
      else {
        Var var = new Var(val);

        _value = var;

        return var;
      }
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
      Value oldValue = _value;

      _value = value;

      return oldValue;
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new RefVar((Var) value);
      else {
        _value = new Var(value);

        return new RefVar((Var) _value);
      }
    }

    /**
     * Converts to a variable reference (for function  arguments)
     */
    public Value toArgRef()
    {
      Value value = _value;

      if (value instanceof Var)
        return new RefVar((Var) value);
      else {
        _value = new Var(_value);

        return new RefVar((Var) _value);
      }
    }

    public Value toArg()
    {
      // php/39a4
      Value value = _value;

      // php/39aj
      if (value instanceof Var)
        return value;
      else {
        _value = new Var(value);

        return _value;
      }
    }

    public void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 2 * depth);
      out.print("[");

      if (_key instanceof StringValue)
        out.print("\"" + _key + "\"");
      else
        out.print(_key);

      out.println("]=>");

      printDepth(out, 2 * depth);

      _value.varDump(env, out, depth, valueSet);
    }

    protected void printRImpl(Env env,
                              WriteStream out,
                              int depth,
                              IdentityHashMap<Value, String> valueSet)
      throws IOException
    {
      printDepth(out, 8 * depth);
      out.print("    [");
      out.print(_key);
      out.print("] => ");
      if (_value != null)
        _value.printR(env, out, depth + 1, valueSet);
      out.println();
    }

    private void printDepth(WriteStream out, int depth)
      throws java.io.IOException
    {
      for (int i = depth; i > 0; i--)
        out.print(' ');
    }

    public String toString()
    {
      return "ArrayValue.Entry[" + getKey() + "]";
    }
  }

  /**
   * Takes the values of this array and puts them in a java array
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

  /**
   * Takes the values of this array, unmarshals them to objects of type
   * <i>elementType</i>, and puts them in a java array.
   */
  public Object valuesToArray(Env env, Class elementType)
  {
    int size = getSize();

    Object array = Array.newInstance(elementType, size);

    MarshalFactory factory = env.getModuleContext().getMarshalFactory();
    Marshal elementMarshal = factory.create(elementType);

    int i = 0;

    for (Entry ptr = getHead(); ptr != null; ptr = ptr.getNext()) {
      Array.set(array, i++, elementMarshal.marshal(env,
                                                   ptr.getValue(),
                                                   elementType));
    }

    return array;
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

  public static class ValueComparator
    implements Comparator<Map.Entry<Value,Value>>
  {
    public static final ValueComparator CMP = new ValueComparator();
    
    private ValueComparator()
    {
    }
    
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

  public static class KeyComparator
    implements Comparator<Map.Entry<Value,Value>>
  {
    public static final KeyComparator CMP = new KeyComparator();
    
    private KeyComparator()
    {
    }
    
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

  public static class GetKey extends AbstractGet
  {
    public static final GetKey GET = new GetKey();
    
    private GetKey()
    {
    }
    
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getKey();
    }
  }

  public static class GetValue extends AbstractGet {
    public static final GetValue GET = new GetValue();
    
    private GetValue()
    {
    }
    
    public Value get(Map.Entry<Value, Value> entry)
    {
      return entry.getValue();
    }
  }
}



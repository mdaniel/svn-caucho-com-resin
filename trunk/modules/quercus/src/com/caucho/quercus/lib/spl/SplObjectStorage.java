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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.spl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusLanguageException;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.VariableModule;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

public class SplObjectStorage
  implements ArrayAccess, Countable, Iterator, Serializable
{
  private static L10N L = new L10N(SplObjectStorage.class);

  private HashMap<StringValue,ObjectEntry> _objMap
    = new HashMap<StringValue,ObjectEntry>();

  private ObjectEntry _head;
  private ObjectEntry _tail;

  private int _iterCount;
  private ObjectEntry _current;

  public void addAll(Env env, SplObjectStorage storage)
  {
    ObjectEntry entry = storage._head;

    while (entry != null) {
      attachImpl(env, entry.getHash(), entry.getObject(), entry.getValue());

      entry = entry.getNext();
    }
  }

  public void attach(Env env, Value obj, @Optional Value value)
  {
    StringValue hash = getHash(env, obj);

    attachImpl(env, hash, obj, value);
  }

  private void attachImpl(Env env, StringValue hash, Value obj, Value value)
  {
    ObjectEntry entry = _objMap.get(hash);

    if (entry == null) {
      entry = new ObjectEntry(hash, obj, value);

      if (_tail != null) {
        _tail.setNext(entry);

        entry.setPrev(_tail);
      }

      _tail = entry;

      if (_head == null) {
        _head = entry;
      }
    }
    else {
      entry.setObject(obj);
      entry.setValue(value);
    }

    _objMap.put(hash, entry);
  }

  public boolean contains(Env env, Value obj)
  {
    StringValue hash = getHash(env, obj);

    return _objMap.containsKey(hash);
  }

  @Override
  public int count(Env env)
  {
    return _objMap.size();
  }

  public void detach(Env env, Value obj)
  {
    StringValue hash = getHash(env, obj);

    detachImpl(env, hash);
  }

  private void detachImpl(Env env, StringValue hash)
  {
    ObjectEntry entry = _objMap.remove(hash);

    if (entry == null) {
      return;
    }

    ObjectEntry prev = entry.getPrev();
    ObjectEntry next = entry.getNext();

    if (prev != null) {
      prev.setNext(next);
    }

    if (next != null) {
      next.setPrev(prev);
    }

    if (entry == _tail) {
      _tail = entry.getPrev();
    }

    if (entry == _head) {
      _head = entry.getNext();
    }
  }

  //
  // ArrayAccess
  //

  @Override
  public boolean offsetExists(Env env, Value offset)
  {
    StringValue hash = offset.toObject(env).getObjectHash(env);

    return _objMap.containsKey(hash);
  }

  @Override
  public Value offsetSet(Env env, Value obj, Value value)
  {
    // XXX: value is optional

    attach(env, obj, value);

    return NullValue.NULL;
  }

  @Override
  public Value offsetGet(Env env, Value obj)
  {
    StringValue hash = getHash(env, obj);

    ObjectEntry entry = _objMap.get(hash);

    if (entry == null) {
      Value e = env.createException("UnexpectedValueException", L.l("{0} not found", hash));

      throw new QuercusLanguageException(e);
    }

    return entry.getValue();
  }

  @Override
  public Value offsetUnset(Env env, Value obj)
  {
    detach(env, obj);

    return NullValue.NULL;
  }

  //
  // Iterator
  //

  @Override
  public Value current(Env env)
  {
    if (_current == null) {
      return NullValue.NULL;
    }

    return _current.getObject();
  }

  @Override
  public Value key(Env env)
  {
    return LongValue.create(_iterCount);
  }

  @Override
  public void next(Env env)
  {
    if (_current != null) {
      _current = _current.getNext();

      _iterCount++;
    }
  }

  @Override
  public void rewind(Env env)
  {
    _current = _head;

    _iterCount = 0;
  }

  @Override
  public boolean valid(Env env)
  {
    return _current != null;
  }

  public StringValue getHash(Env env, Value obj)
  {
    return obj.getObjectHash(env);
  }

  public Value getInfo()
  {
    if (_current == null) {
      return NullValue.NULL;
    }

    return _current.getValue();
  }

  public void setInfo(Value value)
  {
    if (_current == null) {
      return;
    }

    _current.setValue(value);
  }

  public void removeAll(Env env, SplObjectStorage storage)
  {
    for (StringValue hash : storage._objMap.keySet()) {
      detachImpl(env, hash);
    }
  }

  public void removeAllExcept(Env env, SplObjectStorage storage)
  {
    ArrayList<ObjectEntry> toRemoveList = new ArrayList<ObjectEntry>();

    ObjectEntry entry = _head;

    while (entry != null) {
      if (storage._objMap.containsKey(entry.getHash())) {
      }
      else {
        toRemoveList.add(entry);
      }

      entry = entry.getNext();
    }

    for (ObjectEntry listEntry : toRemoveList) {
      detachImpl(env, listEntry.getHash());
    }
  }

  @Override
  public StringValue serialize(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    StringValue objStr = env.createString("obj");
    StringValue valueStr = env.createString("inf");

    ObjectEntry entry = _head;

    while (entry != null) {
      ArrayValue inner = new ArrayValueImpl();

      inner.put(objStr, entry.getObject());
      inner.put(valueStr, entry.getValue());

      array.append(inner);

      entry = entry.getNext();
    }

    String str = VariableModule.serialize(env, array);

    return env.createString(str);
  }

  @Override
  public void unserialize(Env env, StringValue str)
  {
    Value unserializedValue = VariableModule.unserialize(env, str);

    ArrayValue array = unserializedValue.toArrayValue(env);

    StringValue objStr = env.createString("obj");
    StringValue valueStr = env.createString("inf");

    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      ArrayValue inner = entry.getValue().toArrayValue(env);

      Value obj = inner.get(objStr);
      Value value = inner.get(valueStr);

      attach(env, obj, value);
    }
  }

  private ArrayValue toArrayValue(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    StringValue objStr = env.createString("obj");
    StringValue valueStr = env.createString("inf");

    for (Map.Entry<StringValue,ObjectEntry> entry : _objMap.entrySet()) {
      ArrayValue inner = new ArrayValueImpl();

      inner.put(objStr, entry.getValue().getObject());
      inner.put(valueStr, entry.getValue().getValue());

      array.put(entry.getKey(), inner);
    }

    return array;
  }

  public void varDumpImpl(Env env,
                          Value obj,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    ArrayValue array = toArrayValue(env);

    array.varDump(env, out, depth, valueSet);
  }

  static class ObjectEntry {
    private final StringValue _hash;

    private Value _obj;
    private Value _value;

    private ObjectEntry _prev;
    private ObjectEntry _next;

    public ObjectEntry(StringValue hash, Value obj, Value value)
    {
      _hash = hash;

      _obj = obj;
      _value = value;
    }

    public StringValue getHash()
    {
      return _hash;
    }

    public Value getObject()
    {
      return _obj;
    }

    public void setObject(Value obj)
    {
      _obj = obj;
    }

    public Value getValue()
    {
      return _value;
    }

    public void setValue(Value value)
    {
      _value = value;
    }

    public ObjectEntry getPrev()
    {
      return _prev;
    }

    public void setPrev(ObjectEntry entry)
    {
      _prev = entry;
    }

    public ObjectEntry getNext()
    {
      return _next;
    }

    public void setNext(ObjectEntry entry)
    {
      _next = entry;
    }
  }
}

/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.caucho.util.IntMap;

/**
 * Represents the Quercus environment.
 */
public class ProSymbolMap extends AbstractMap<StringValue,EnvVar> {
  private final IntMap _intMap;
  private final Var []_values;
  
  private HashMap<StringValue,EnvVar> _extMap;

  public ProSymbolMap(IntMap intMap, Var []values)
  {
    _intMap = intMap;
    _values = values;
    
    /*
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null)
        throw new NullPointerException(i + " is null");
    }
    */
  }

  /**
   * Returns the matching value, or null.
   */
  @Override
  public EnvVar get(Object key)
  {
    return (EnvVar) get((StringValue) key);
  }

  /**
   * Returns the matching value, or null.
   */
  public EnvVar get(StringValue key)
  {
    int index = _intMap.get(key);
    
    if (index >= 0)
      return new SymbolEnvVar(_values, index);
    else if (_extMap != null)
      return _extMap.get(key);
    else
      return null;
  }

  /**
   * Returns the matching value, or null.
   */
  public final Var getVar(StringValue key)
  {
    int index = _intMap.get(key);

    if (index >= 0)
      return _values[index];
    else if (_extMap != null) {
      EnvVar envVar = _extMap.get(key);

      if (envVar != null)
        return envVar.getVar();
    }
    
    return null;
  }

  /**
   * Returns the matching value, or null.
   */
  @Override
  public EnvVar put(StringValue key, EnvVar newVar)
  {
    int index = _intMap.get(key);

    if (index >= 0) {
      throw new IllegalStateException();
    }

    if (_extMap == null)
      _extMap = new HashMap<StringValue,EnvVar>(8);

    return _extMap.put(key, newVar);
  }

  public Set<Map.Entry<StringValue,EnvVar>> entrySet()
  {
    HashSet<Map.Entry<StringValue,EnvVar>> set
      = new HashSet<Map.Entry<StringValue,EnvVar>>();

    Iterator iter = _intMap.iterator();
    while (iter.hasNext()) {
      StringValue key = (StringValue) iter.next();
      int index = _intMap.get(key);

      set.add(new EntryImpl(key, new SymbolEnvVar(_values, index)));
    }

    return set;
  }

  static final class SymbolEnvVar extends EnvVar {
    private final Var []_values;
    private final int _index;

    SymbolEnvVar(Var []values, int index)
    {
      _values = values;
      _index = index;
    }

    @Override
    public final Value get()
    {
      Var var = _values[_index];
      
      if (var != null)
        return var.toValue();
      else
        return NullValue.NULL;
    }

    @Override
    public Var getVar()
    {
      return _values[_index];
    }

    @Override
    public final Value set(Value value)
    {
      Var var = _values[_index];
      
      if (var == null) {
        var = new Var();
        _values[_index] = var;
      }
      
      var.set(value);

      return value;
    }

    @Override
    public Var setRef(Value value)
    {
      Var var;
      
      if (value.isVar()) {
        var = (Var) value;

        _values[_index] = var;
      }
      else {
        var = _values[_index];
        
        if (var == null) {
          var = new Var();
          _values[_index] = var;
        }

        var.set(value);
      }
      
      return var;
    }

    @Override
    public Var setVar(Var var)
    {
      _values[_index] = var;

      return var;
    }
  }

  static class EntryImpl implements Map.Entry<StringValue,EnvVar> {
    private final StringValue _key;
    private final EnvVar _envVar;

    EntryImpl(StringValue key, EnvVar var)
    {
      _key = key;
      _envVar = var;
    }

    public StringValue getKey()
    {
      return _key;
    }

    public EnvVar getValue()
    {
      return _envVar;
    }

    public EnvVar setValue(EnvVar var)
    {
      throw new UnsupportedOperationException();
    }
  }
}


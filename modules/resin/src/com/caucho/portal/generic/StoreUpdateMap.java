/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */

package com.caucho.portal.generic;

import java.io.IOException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StoreUpdateMap<K, V> implements Map<K, V>
{
  static protected final Logger log = 
    Logger.getLogger(StoreUpdateMap.class.getName());

  private Map<K, K> _nameLinkMap;
  private Map<K, K> _reverseNameLinkMap;
  private Map<K, V> _storeMapUnlinked;
  private Map<K, V> _storeMap;
  private Map<K, V> _defaultMap;
  private Set<K> _names;
  private V _deletedValue;

  private Set<K> _keySet;
  private boolean _keySetModifiable;
  private Map<K, V> _updateMapUnlinked;
  private Map<K, V> _updateMap;

  void start( Map<K, K> nameLinkMap,
              Map<K, K> reverseNameLinkMap, 
              Map<K, V> defaultMap, 
              Map<K, V> storeMap, 
              Set<K> names,
              V deletedValue )
  {
    if (_defaultMap != null || _storeMap != null)
      throw new IllegalStateException("missing finish()?");

    _nameLinkMap = nameLinkMap;
    _reverseNameLinkMap = reverseNameLinkMap;

    if (nameLinkMap != null) {
      _defaultMap = new KeyLinkMap<K, V>( defaultMap, 
                                          _nameLinkMap, 
                                          _reverseNameLinkMap );
      _storeMapUnlinked = storeMap;
      _storeMap = new KeyLinkMap<K, V>( storeMap,
                                        _nameLinkMap, 
                                        _reverseNameLinkMap );
    }
    else {
      _defaultMap = defaultMap;
      _storeMapUnlinked = storeMap;
      _storeMap = storeMap;
    }

    _names = names;
    _deletedValue = deletedValue;
  }

  void finish()
  {
    _updateMap = null;
    _updateMapUnlinked = null;
    _keySet = null;
    _keySetModifiable = false;

    _nameLinkMap = null;
    _defaultMap = null;
    _storeMapUnlinked = null;
    _storeMap = null;
    _names = null;
  }

  Map<K, V> getStoreMap()
  {
    return _storeMapUnlinked;
  }

  Map<K, V> getUpdateMap()
  {
    return _updateMapUnlinked;
  }

  private Set<K> getKeySet(boolean modifiable)
  {

    if (!modifiable) {
      if (_keySet != null)
        return _keySet;

      while (true) {
        Set<K> keySet = _names;

        if (_defaultMap != null) {
          if (keySet != null)
            break;
          else
            keySet = _defaultMap.keySet();
        }

        if (_storeMap != null) {
          if (keySet != null)
            break;
          else
            keySet = _storeMap.keySet();
        }

        if (_updateMap != null) {
          if (keySet != null)
            break;
          else
            keySet = _updateMap.keySet();
        }
      }
    }

    if (_keySetModifiable)
      return _keySet;

    _keySet = new HashSet<K>();
    _keySetModifiable = true;

    if (_names != null) {
      Iterator<K> iter = _names.iterator();

      while (iter.hasNext()) {
        K key = iter.next();

        if ((_storeMap != null && _storeMap.containsKey(key))
            ||
            (_defaultMap != null && _defaultMap.containsKey(key)))
        {
          _keySet.add(key);
        }
      }
    }
    else {
      if (_defaultMap != null)
        _keySet.addAll(_defaultMap.keySet());

      if (_storeMap != null)
        _keySet.addAll(_storeMap.keySet());
    }


    return _keySet;
  }

  public int size()
  {
    return getKeySet(false).size();
  }

  public boolean isEmpty()
  {
    return getKeySet(false).isEmpty();
  }

  public boolean containsKey(Object key)
  {
    return getKeySet(false).contains(key);
  }

  public V get(Object key)
  {
    if (!getKeySet(false).contains(key))
      return null;

    if (_updateMap != null && _updateMap.containsKey(key)) {
      V v = _updateMap.get(key);

      return v;
    } else if (_storeMap != null && _storeMap.containsKey(key))
      return _storeMap.get(key);
    else if (_defaultMap != null)
      return _defaultMap.get(key);
    else
      return null;
  }

  public Set<K> keySet()
  {
    return getKeySet(false);
  }

  public boolean containsValue(Object v)
  {
    Iterator<K> iter = _keySet.iterator();

    while (iter.hasNext()) {
      K key = iter.next();
      V value = get(key);

      if (value == null) {
        if (v == null)
          return true;
      }
      else {
        if (value.equals(v))
          return true;
      }
    }

    return false;
  }

  public Collection<V> values()
  {
    LinkedList<V> values = new LinkedList<V>();

    Iterator<K> iter = getKeySet(false).iterator();

    while (iter.hasNext()) {
      K key = iter.next();
      V value = get(key);

      values.add(value);
    }

    return values;
  }

  public Set<Map.Entry<K, V>> entrySet()
  {
    // XXX: better implemented as custom class extending AbstractSet
    LinkedHashMap<K, V> map = new LinkedHashMap<K, V>();

    Iterator<K> iter = getKeySet(false).iterator();

    while (iter.hasNext()) {
      K key = iter.next();
      V value = get(key);

      map.put(key, value);
    }

    return map.entrySet();
  }

  private void makeUpdateMap()
  {
    if (_updateMap == null) {
      _updateMapUnlinked = new LinkedHashMap<K, V>();
      if (_nameLinkMap != null)
        _updateMap = new KeyLinkMap<K, V>( _updateMapUnlinked,
                                           _nameLinkMap, 
                                           _reverseNameLinkMap );
      else
        _updateMap = _updateMapUnlinked;
    }
  }

  public V put(K key, V value)
  {
    if (_names != null && !_names.contains(key))
      throw new IllegalArgumentException(
          "attribute name `" + key + "' not allowed");

    makeUpdateMap();

    Set<K> keySet = getKeySet(false);

    if (!keySet.contains(key)) {
      if  (keySet != _updateMap.keySet())
        keySet = getKeySet(true);

      if (value != _deletedValue)
        keySet.add(key);
    }

    V oldValue = _updateMap.put(key, value);

    if (oldValue == _deletedValue)
      oldValue = null;

    return oldValue;
  }

  public void putAll(Map<? extends K, ? extends V> srcMap) 
  {
    Iterator<? extends Entry<? extends K, ? extends V>> iter
      = srcMap.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry<? extends K, ? extends V> entry = iter.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public V remove(Object key)
  { 
    Set<K> keySet = getKeySet(false);

    V oldValue = null;

    if (keySet.contains(key)) {
      oldValue = put( (K) key, _deletedValue);
      getKeySet(true).remove(key);
    }

    return oldValue;
  }

  public void clear()
  {
    Iterator<K> iter = getKeySet(true).iterator();

    while (iter.hasNext()) {
      K key = iter.next();

      if (_storeMap != null && _storeMap.containsKey(key))
        put(key, _deletedValue);
      else if (_updateMap != null)
        _updateMap.remove(key);

      iter.remove();
    }
  }
}


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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A Map that wraps another map and uses different keys in the wrapped
 * map than the keys that are exposed.
 *
 * The constructor is passed the map to be wrapped, and a keyLinkMap
 * that maps keys as they are used by users of the map to different keys that
 * are really used in the wrapped map.  If a key is used that is not int the
 * keyLinkMap, the key is used unchanged in the wrapped map. 
 */
public class KeyLinkMap<K, V> implements Map<K, V>
{
  static protected final Logger log = 
    Logger.getLogger(KeyLinkMap.class.getName());

  private Map<K, K> _keyLinkMap;
  private Map<K, K> _keyLinkReverseMap;
  private Map<K, V> _map;

  private KeySet _keySet;
  private EntrySet _entrySet;

  public static <K> Map<K, K> getReverseKeyLinkMap(Map<K, K> keyLinkMap)
  {
    Map<K, K> keyLinkReverseMap = new HashMap<K, K>();
    Iterator<Map.Entry<K, K>> iter = keyLinkMap.entrySet().iterator();

    while (iter.hasNext()) {
      Map.Entry<K, K> entry = iter.next();

      keyLinkReverseMap.put(entry.getValue(), entry.getKey());
    }

    return keyLinkReverseMap;
  }

  public KeyLinkMap( Map<K, V> map, 
                     Map<K, K> keyLinkMap, 
                     Map<K, K> keyLinkReverseMap )
  {
    _map = map;
    _keyLinkMap = keyLinkMap;
    _keyLinkReverseMap = keyLinkReverseMap;
  }

  
  public int size()
  {
    return _map.size();
  }

  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  private K getMapKey(K key)
  {
    K mapKey = key;

    if (_keyLinkMap != null) {
      mapKey = _keyLinkMap.get(key);
      if (mapKey == null)
        mapKey = key;
    }

    return mapKey;
  }

  private K getReverseMapKey(K key)
  {
    if (_keyLinkReverseMap == null)
      return key;

    K mapKey = key;

    mapKey = _keyLinkReverseMap.get(key);

    if (mapKey == null)
      mapKey = key;

    return mapKey;
  }

  public boolean containsKey(Object key)
  {
    return _map.containsKey(getMapKey( (K) key ));
  }

  public V get(Object key)
  {
    return _map.get(getMapKey( (K) key ));
  }

  public Set<K> keySet()
  {
    return _keySet == null ? (_keySet = new KeySet()) : _keySet;
  }

  public Set<Map.Entry<K, V>> entrySet()
  {
    return _entrySet == null ? (_entrySet = new EntrySet()) : _entrySet;
  }

  public boolean containsValue(Object v)
  {
    return _map.containsValue(v);
  }

  public Collection<V> values()
  {
    return _map.values();
  }

  public V put(K key, V value)
  {
    return _map.put(getMapKey(key), value);
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
    return _map.remove(getMapKey( (K) key ));
  }

  public void clear()
  {
    _map.clear();
  }

  private Iterator<K> newKeyIterator()
  {
    if (_keyLinkMap == null)
      return _map.keySet().iterator();
    else
      return new KeyIterator();

  }

  private Iterator<Map.Entry<K, V>> newEntryIterator()
  {
    if (_keyLinkMap == null)
      return _map.entrySet().iterator();
    else
      return new EntryIterator();
  }

  private class KeySet extends AbstractSet<K> {
    public Iterator<K> iterator() 
    {
      return newKeyIterator();
    }

    public int size() 
    {
      return KeyLinkMap.this.size();
    }

    public boolean contains(Object o) 
    {
      return KeyLinkMap.this.containsKey(o);
    }

    public boolean remove(Object o) 
    {
      return KeyLinkMap.this.remove(o) != null;
    }

    public void clear() 
    {
      KeyLinkMap.this.clear();
    }
  }

  private class EntrySet extends AbstractSet<Map.Entry<K,V>> 
  {
    public Iterator<Map.Entry<K,V>> iterator() 
    {
      return newEntryIterator();
    }

    public boolean contains(Object o) 
    {
      if (!(o instanceof Map.Entry))
        return false;

      Map.Entry<K,V> other = (Map.Entry<K,V>) o;

      V value = KeyLinkMap.this.get(other.getKey());

      if (value == null)
        return other.getValue() == null;
      else
        return value.equals(other.getValue());
    }

    public boolean remove(Object o) 
    {
      return KeyLinkMap.this.remove(o) != null;
    }

    public int size() 
    {
      return KeyLinkMap.this.size();
    }

    public void clear() 
    {
      KeyLinkMap.this.clear();
    }
  }

  private class KeyIterator implements Iterator<K> 
  {
    private Iterator<K> _iterator;

    KeyIterator()
    {
      _iterator = _map.keySet().iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public K next()
    {
      K next = _iterator.next();
      K mapKey = KeyLinkMap.this.getReverseMapKey(next);

      return mapKey;
    }

    public void remove()
    {
      _iterator.remove();
    }
  }

  private class EntryIterator implements Iterator<Map.Entry<K, V>> 
  {
    private Iterator<Map.Entry<K, V>> _iterator;

    EntryIterator()
    {
      _iterator = _map.entrySet().iterator();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public Map.Entry<K, V> next()
    {
      Map.Entry<K, V> nextEntry = _iterator.next();
      K nextKey = nextEntry.getKey();

      K mapKey = KeyLinkMap.this.getReverseMapKey(nextEntry.getKey());

      if (mapKey == nextKey)
        return nextEntry;
      else
        return new MapEntry<K, V>(mapKey, nextEntry.getValue());
    }

    public void remove()
    {
      _iterator.remove();
    }
  }

  private class MapEntry<K, V> implements Map.Entry<K, V>
  {
    private K _key;
    private V _value;

    MapEntry(K key, V value)
    {
      _key = key;
      _value = value;
    }

    public K getKey()
    {
      return _key;
    }

    public K setKey(K key)
    {
      K oldKey = _key;
      _key = key;
      return oldKey;
    }

    public V getValue()
    {
      return _value;
    }

    public V setValue(V value)
    {
      V oldValue = _value;
      _value = value;
      return oldValue;
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof Map.Entry))
        return false;

      Map.Entry<K, V> other = (Map.Entry<K, V>) o;

      return 
        (getKey() == null 
         ? other.getKey() == null 
         : getKey().equals(other.getKey()))
        &&
        (getValue() == null 
         ? other.getValue() == null 
         : getValue().equals(other.getValue()));
    }

    public int hashCode()
    {
      return 
        (getKey() == null   ? 0 : getKey().hashCode()) ^
        (getValue() == null ? 0 : getValue().hashCode());
    }
  }
}


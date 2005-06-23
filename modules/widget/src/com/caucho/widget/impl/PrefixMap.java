/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget.impl;

import com.caucho.util.L10N;

import java.util.*;

/**
 * A map that is a view of a backing map, a String prefix disappears from the
 * keys in the backing map. Changes to this map are reflected in the backing map,
 * with appropriate modification of the key for the current prefix.
 */
public class PrefixMap
  implements Map<String, String[]>
{
  private static final L10N L = new L10N(PrefixMap.class);

  private Set<String> _keySet = new KeySet();
  private Set<Entry<String, String[]>> _entrySet = new EntrySet();

  private Map<String, String[]> _backingMap;
  private String  _prefix;

  private StringBuilder _prefixBuilder = new StringBuilder();
  private int _prefixLen;

  private boolean _isInit;

  public PrefixMap()
  {
  }

  public PrefixMap(String prefix, Map<String, String[]> backingMap)
  {
    setPrefix(prefix);
    setBackingMap(backingMap);
    init();
  }

  public void setBackingMap(Map<String, String[]> backingMap)
  {
    _backingMap = backingMap;

    if (_isInit)
      initImpl();
  }

  private Map<String, String[]> getBackingMap()
  {
    return _backingMap;
  }

  public void setPrefix(String prefix)
  {
    _prefix = prefix;

    if (_isInit)
      initImpl();
  }

  public String getPrefix()
  {
    return _prefix;
  }

  public boolean isInit()
  {
    return _isInit;
  }

  public void init()
  {
    assert !_isInit;

    if (_backingMap == null)
      throw new IllegalStateException(L.l("`{0}' is required", "backing-map"));

    initImpl();

    _isInit = true;
  }

  private void initImpl()
  {
    _prefixBuilder.setLength(0);

    if (_prefix != null)
      _prefixBuilder.append(_prefix);

    // optimization
    if (_backingMap instanceof PrefixMap) {
      PrefixMap parentMap = this;

      do {
        parentMap = (PrefixMap) parentMap.getBackingMap();

        String parentPrefix = parentMap.getPrefix();

        if (parentPrefix != null)
          _prefixBuilder.insert(0, parentPrefix);

        _backingMap = parentMap.getBackingMap();

      } while (_backingMap instanceof PrefixMap);

      _prefix = _prefixBuilder.toString();
    }

    _prefixLen = _prefixBuilder.length();
  }

  public void destroy()
  {
    _backingMap = null;
    _prefix = null;

    _prefixBuilder.setLength(0);

    _isInit = false;
  }

  private String createBackingKey(Object localKey)
  {
    _prefixBuilder.setLength(_prefixLen);
    _prefixBuilder.append(localKey.toString());

    return _prefixBuilder.toString();
  }

  private String createLocalKey(String backingKey)
  {
    if (backingKey == null || _prefixLen == 0)
      return backingKey;
    else
      return backingKey.substring(_prefix.length());
  }

  private boolean isBackingKeyMatch(String backingKey)
  {
    if (backingKey == null)
      return _prefixLen == 0;
    else if (_prefixLen == 0)
      return true;
    else
      return backingKey.startsWith(_prefix);
  }

  public String[] get(Object key)
  {
    return _backingMap.get(createBackingKey(key));
  }

  public String[] put(String key, String[] value)
  {
    String backingKey = createBackingKey(key);

    return _backingMap.put(backingKey, value);
  }

  public void putAll(Map<? extends String, ? extends String[]> map)
  {
    for (Entry<? extends String, ? extends String[]> entry : map.entrySet())
      put(entry.getKey(), entry.getValue());
  }

  public void clear()
  {
    Iterator<Entry<String, String[]>> entryIterator = _backingMap.entrySet().iterator();

    while (entryIterator.hasNext()) {
      Entry<String, String[]> entry = entryIterator.next();
      String backingKey = entry.getKey();

      if (isBackingKeyMatch(backingKey))
        entryIterator.remove();
    }
  }

  public Collection<String[]> values()
  {
    LinkedList<String[]> values = new LinkedList<String[]>();

    Iterator<Entry<String, String[]>> entryIterator = _backingMap.entrySet().iterator();

    while (entryIterator.hasNext()) {
      Entry<String, String[]> entry = entryIterator.next();
      String backingKey = entry.getKey();
      String[] value = entry.getValue();

      if (isBackingKeyMatch(backingKey))
        values.add(value);
    }

    return values;
  }

  public boolean containsKey(Object key)
  {
    return _backingMap.containsKey(createBackingKey(key));
  }

  public boolean containsValue(Object value)
  {
    Iterator<Entry<String, String[]>> entryIterator = _backingMap.entrySet().iterator();

    while (entryIterator.hasNext()) {
      Entry<String, String[]> entry = entryIterator.next();
      String backingKey = entry.getKey();
      String[] backingValue = entry.getValue();

      if (isBackingKeyMatch(backingKey)) {

        boolean isMatch = value == null ? backingValue == null : value.equals(backingValue);

        if (isMatch)
          return true;
      }
    }

    return false;
  }

  public String[] remove(Object key)
  {
    return _backingMap.remove(createBackingKey(key));
  }

  public Set<String> keySet()
  {
    return _keySet;
  }

  public int size()
  {
    int size = 0;

    for (String backingKey : _backingMap.keySet())
      if (isBackingKeyMatch(backingKey))
        size++;

    return size;
  }

  public boolean isEmpty()
  {
    for (String backingKey : _backingMap.keySet())
      if (isBackingKeyMatch(backingKey))
        return false;

    return true;
  }

  public Set<Entry<String, String[]>> entrySet()
  {
    return _entrySet;
  }

  public boolean isPrefix()
  {
    return _prefixLen > 0;
  }

  private class KeySet
    extends AbstractSet<String>
  {
    public Iterator<String> iterator()
    {
      return new KeyIterator();
    }

    public int size()
    {
      return PrefixMap.this.size();
    }

    public boolean isEmpty()
    {
      return PrefixMap.this.isEmpty();
    }
  }

  private class KeyIterator
    implements Iterator<String>
  {
    private Iterator<String> _backingIterator = _backingMap.keySet().iterator();
    private String _nextBackingKey = null;

    public KeyIterator()
    {
      advanceToNext();
    }

    private void advanceToNext()
    {
      _nextBackingKey = null;

      while (_backingIterator.hasNext()) {
        String backingKey = _backingIterator.next();

        if (isBackingKeyMatch(backingKey)) {
          _nextBackingKey = backingKey;
          break;
        }
      }
    }

    public boolean hasNext()
    {
      return _nextBackingKey != null;
    }

    public String next()
    {
      if (_nextBackingKey == null)
        throw new NoSuchElementException();

      String next = createLocalKey(_nextBackingKey);

      advanceToNext();

      return next;
    }

    public void remove()
    {
      _backingIterator.remove();
    }
  }

  private class EntrySet
    extends AbstractSet<Entry<String, String[]>>
  {
    public Iterator<Entry<String, String[]>> iterator()
    {
      return new EntryIterator();
    }

    public int size()
    {
      return PrefixMap.this.size();
    }

    public boolean isEmpty()
    {
      return PrefixMap.this.isEmpty();
    }
  }

  private class EntryIterator
    implements Iterator<Entry<String, String[]>>
  {
    private Iterator<Entry<String, String[]>> _backingIterator = _backingMap.entrySet().iterator();
    private Entry<String, String[]> _next = null;

    public EntryIterator()
    {
      advanceToNext();
    }

    private void advanceToNext()
    {
      _next = null;

      while (_backingIterator.hasNext()) {
        Entry<String, String[]> next = _backingIterator.next();

        String backingKey = next.getKey();

        if (backingKey != null && isBackingKeyMatch(backingKey)) {
          _next = next;
          break;
        }
      }
    }

    public boolean hasNext()
    {
      return _next != null;
    }

    public Entry<String, String[]> next()
    {
      if (_next == null)
        throw new NoSuchElementException();

      String key = _next.getKey();

      String keyWithoutPrefix = key.substring(_prefix.length());

      advanceToNext();

      return new PrefixMapEntry(keyWithoutPrefix);
    }

    public void remove()
    {
      _backingIterator.remove();
    }
  }

  private class PrefixMapEntry
    implements Map.Entry<String, String[]>, Comparable<PrefixMapEntry>
  {
    private String _keyWithoutPrefix;

    public PrefixMapEntry(String keyWithoutPrefix)
    {
      _keyWithoutPrefix = keyWithoutPrefix;
    }

    public String getKey()
    {
      return _keyWithoutPrefix;
    }

    public String[] getValue()
    {
      return get(_keyWithoutPrefix);
    }

    public String[] setValue(String[] value)
    {
      return put(_keyWithoutPrefix, value);
    }

    public int compareTo(PrefixMapEntry other)
    {
      if (_keyWithoutPrefix == null)
        return -1;
      else if (other == null)
        return 1;

      String otherKey = other.getKey();

      if (otherKey == null)
        return 1;

      return _keyWithoutPrefix.compareTo(otherKey);
    }
  }
}

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

import junit.framework.TestCase;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeSet;

public class TestPrefixMap
  extends TestCase
{
  private static final String STATE_SEPARATOR = "\n";
  private StringBuilder _state = new StringBuilder();

  private Map<String,String[]> _map;

  public void setUp()
    throws Exception
  {
    _map = new LinkedHashMap<String, String[]>();

    _map.put("foo", new String[] { "foo:value" });
    _map.put("foo.bar", new String[] { "foo.bar:value" } );
    _map.put("foo.bar.baz", new String[] { "foo.bar.baz:value"} );
  }

  public void tearDown()
  {
    _map = null;
  }

  protected void addState(String state)
  {
    if (_state.length() > 0)
      _state.append(STATE_SEPARATOR);

    _state.append(state);
  }

  protected void assertState(String ... expected)
  {
    StringBuilder buf = new StringBuilder();

    for (String expect : expected) {
      if (buf.length() > 0)
        buf.append(STATE_SEPARATOR);

      buf.append(expect);
    }

    String expectedValue = buf.toString();
    String value = _state.toString();

    assertEquals(expectedValue, value);
  }

  public void testNoPrefix()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.init();

    assertEquals("foo:value", prefixMap.get("foo")[0]);
    assertEquals("foo.bar:value", prefixMap.get("foo.bar")[0]);
    assertEquals("foo.bar.baz:value", prefixMap.get("foo.bar.baz")[0]);

    TreeSet<String> sortedKeySet = new TreeSet<String>(prefixMap.keySet());

    for (String key : sortedKeySet) {
      String value = prefixMap.get(key)[0];

      addState(key + "=" + value);
    }

    assertState(
      "foo=foo:value",
      "foo.bar=foo.bar:value",
      "foo.bar.baz=foo.bar.baz:value"
    );
  }

  public void testBackingMapRequired()
  {
    PrefixMap prefixMap = new PrefixMap();

    try {
      prefixMap.init();
    }
    catch (IllegalStateException ex) {
      assertTrue(ex.toString(), ex.getMessage().contains("is required"));
      return;
    }

    fail("expecting exception");
  }

  public void testGet()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    assertEquals("foo.bar:value", prefixMap.get("bar")[0]);
    assertEquals("foo.bar.baz:value", prefixMap.get("bar.baz")[0]);
  }

  public void testKeySet()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    TreeSet<String> sortedKeySet = new TreeSet<String>(prefixMap.keySet());

    for (String key : sortedKeySet) {
      String value = prefixMap.get(key)[0];

      addState(key + "=" + value);
    }

    assertState(
      "bar=foo.bar:value",
      "bar.baz=foo.bar.baz:value"
      );
  }

  public void testKeySetNested()
  {
    PrefixMap firstMap = new PrefixMap();
    firstMap.setBackingMap(_map);
    firstMap.setPrefix("foo.");
    firstMap.init();

    PrefixMap secondMap = new PrefixMap();
    secondMap.setBackingMap(firstMap);
    secondMap.setPrefix("bar.");
    secondMap.init();

    TreeSet<String> sortedKeySet = new TreeSet<String>(secondMap.keySet());

    for (String key : sortedKeySet) {
      String value = secondMap.get(key)[0];

      addState(key + "=" + value);
    }

    assertState(
      "baz=foo.bar.baz:value"
    );
  }

  public void testPut()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    prefixMap.put("bung", new String[] { "foo.bung:value" });

    assertEquals("foo.bung:value", _map.get("foo.bung")[0]);
  }

  public void testPutOverwrite()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    prefixMap.put("bar", new String[] { "foo.bar:overwrite-value" });

    assertEquals("foo.bar:overwrite-value", _map.get("foo.bar")[0]);
  }

  public void testRemove()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    assertNotNull(_map.get("foo.bar"));

    prefixMap.remove("bar");

    assertNull(_map.get("foo.bar"));
  }

  public void testSize()
  {
    PrefixMap prefixMap = new PrefixMap("foo.", _map);

    assertEquals(2, prefixMap.size());
  }

  public void testEntrySet()
  {
    PrefixMap prefixMap = new PrefixMap();
    prefixMap.setBackingMap(_map);
    prefixMap.setPrefix("foo.");
    prefixMap.init();

    TreeSet<Map.Entry<String, String[]>> sortedEntrySet = new TreeSet<Map.Entry<String,String[]>>(prefixMap.entrySet());

    for (Map.Entry<String,String[]> entry : sortedEntrySet) {
      addState(entry.getKey() + "=" + entry.getValue()[0]);
    }

    assertState(
      "bar=foo.bar:value",
      "bar.baz=foo.bar.baz:value"
    );
  }

}

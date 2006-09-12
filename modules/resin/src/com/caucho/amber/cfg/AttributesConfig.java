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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import java.util.HashMap;

/**
 * <attributes> tag in the orm.xml
 */
public class AttributesConfig {

  // elements
  private HashMap<String, IdConfig> _idMap
    = new HashMap<String, IdConfig>();

  private HashMap<String, BasicConfig> _basicMap
    = new HashMap<String, BasicConfig>();

  // XXX: to do ...
  /*
  private EmbeddedIdConfig _embeddedId;
  private ArrayList<VersionConfig> _versionList = new ArrayList<VersionConfig>();
  private ArrayList<ManyToOneConfig> _manyToOneList = new ArrayList<ManyToOneConfig>();
  private ArrayList<OneToManyConfig> _oneToManyList = new ArrayList<OneToManyConfig>();
  private ArrayList<OneToOneConfig> _oneToOneList = new ArrayList<OneToOneConfig>();
  private ArrayList<ManyToManyConfig> _manyToManyList = new ArrayList<ManyToManyConfig>();
  private ArrayList<EmbeddedConfig> _embeddedList = new ArrayList<EmbeddedConfig>();
  private ArrayList<TransientConfig> _transientList = new ArrayList<TransientConfig>();
  */

  /**
   * Adds a new <basic>.
   */
  public void addBasic(BasicConfig basic)
  {
    _basicMap.put(basic.getName(), basic);
  }

  /**
   * Returns the <basic> map.
   */
  public HashMap<String, BasicConfig> getBasicMap()
  {
    return _basicMap;
  }

  /**
   * Returns a <basic> config.
   */
  public BasicConfig getBasic(String name)
  {
    return _basicMap.get(name);
  }

  /**
   * Adds a new <id>.
   */
  public void addId(IdConfig id)
  {
    _idMap.put(id.getName(), id);
  }

  /**
   * Returns the <id> map.
   */
  public HashMap<String, IdConfig> getIdMap()
  {
    return _idMap;
  }

  /**
   * Returns an <id> config.
   */
  public IdConfig getId(String name)
  {
    return _idMap.get(name);
  }
}

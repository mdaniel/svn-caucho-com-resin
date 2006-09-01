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

import java.util.ArrayList;

/**
 * <attributes> tag in the orm.xml
 */
public class AttributesConfig {

  // elements
  private ArrayList<IdConfig> _idList = new ArrayList<IdConfig>();

  // XXX: to do ...
  /*
  private EmbeddedIdConfig _embeddedId;
  private ArrayList<BasicConfig> _basicList = new ArrayList<BasicConfig>();
  private ArrayList<VersionConfig> _versionList = new ArrayList<VersionConfig>();
  private ArrayList<ManyToOneConfig> _manyToOneList = new ArrayList<ManyToOneConfig>();
  private ArrayList<OneToManyConfig> _oneToManyList = new ArrayList<OneToManyConfig>();
  private ArrayList<OneToOneConfig> _oneToOneList = new ArrayList<OneToOneConfig>();
  private ArrayList<ManyToManyConfig> _manyToManyList = new ArrayList<ManyToManyConfig>();
  private ArrayList<EmbeddedConfig> _embeddedList = new ArrayList<EmbeddedConfig>();
  private ArrayList<TransientConfig> _transientList = new ArrayList<TransientConfig>();
  */

  /**
   * Adds a new <id>.
   */
  public void addId(IdConfig id)
  {
    _idList.add(id);
  }

  /**
   * Returns the id.
   */
  public ArrayList<IdConfig> getIdList()
  {
    return _idList;
  }
}

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

package com.caucho.amber.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassDependency;
import com.caucho.bytecode.JField;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.make.ClassDependency;

import com.caucho.java.JavaWriter;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.AmberCompletion;

import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.AmberFieldCompare;
import com.caucho.amber.field.EntityManyToOneField;
import com.caucho.amber.field.Id;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.VersionField;

import com.caucho.amber.idgen.IdGenerator;
import com.caucho.amber.idgen.SequenceIdGenerator;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.manager.AmberConnection;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;

/**
 * Represents an embeddable type
 */
public class EmbeddableType extends AbstractStatefulType {
  private static final Logger log = Logger.getLogger(EmbeddableType.class.getName());
  private static final L10N L = new L10N(EmbeddableType.class);

  public EmbeddableType(AmberPersistenceUnit amberPersistenceUnit)
  {
    super(amberPersistenceUnit);
  }

  /**
   * Printable version of the entity.
   */
  public String toString()
  {
    return "EmbeddableType[" + _beanClass.getName() + "]";
  }
}

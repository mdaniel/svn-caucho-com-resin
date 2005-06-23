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

package com.caucho.widget;

import junit.framework.TestCase;

import java.util.Locale;

public class TestVarDefinition
  extends TestCase
{
  public void testDefaults()
  {
    String name = "foo";

    VarDefinition varDefinition = new VarDefinition(name, Object.class);

    assertEquals("No description available.", varDefinition.getDescription(Locale.getDefault()));
    assertEquals(false, varDefinition.isValue());
    assertEquals(null, varDefinition.getValue());
    assertEquals(true, varDefinition.isAllowNull());
    assertEquals(false, varDefinition.isInherited());
    assertEquals(false, varDefinition.isReadOnly());
  }

  public void testDescription()
  {
    String name = "foo";
    String description = "description";

    VarDefinition varDefinition = new VarDefinition(name, Object.class);

    Locale xxyyzzLocale = new Locale("xx", "yy", "zz");

    varDefinition.setDescription(description);

    assertEquals(description, varDefinition.getDescription(xxyyzzLocale));
  }

  public void testDescriptionWithSpecificLocale()
  {
    String name = "foo";
    String xxyyzzDescription = "xx_yy_zz description";

    Locale xxyyzzLocale = new Locale("xx", "yy", "zz");
    Locale aabbccLocale = new Locale("aa", "bb", "cc");

    VarDefinition varDefinition = new VarDefinition(name, Object.class);

    varDefinition.setDescription(xxyyzzLocale, xxyyzzDescription);

    assertEquals(xxyyzzDescription, varDefinition.getDescription(xxyyzzLocale));
    assertEquals(xxyyzzDescription, varDefinition.getDescription(aabbccLocale));
  }

  public void testDescriptionWithDefaultAndSpecificLocale()
  {
    String name = "foo";
    String description = "description";
    String xxyyzzDescription = "xx_yy_zz description";

    Locale xxyyzzLocale = new Locale("xx", "yy", "zz");
    Locale aabbccLocale = new Locale("aa", "bb", "cc");

    VarDefinition varDefinition = new VarDefinition(name, Object.class);

    varDefinition.setDescription(description);
    varDefinition.setDescription(xxyyzzLocale, xxyyzzDescription);

    assertEquals(description, varDefinition.getDescription(aabbccLocale));
    assertEquals(xxyyzzDescription, varDefinition.getDescription(xxyyzzLocale));
  }

  public void testDescriptionLocaleBestMatch()
  {
    String name = "foo";

    String description = "description";
    String xxDescription = "xx description";
    String xxyyDescription = "xx_yy description";

    Locale xxLocale = new Locale("xx");
    Locale xxyyLocale = new Locale("xx", "yy");
    Locale xxyyaaLocale = new Locale("xx", "yy", "aa");
    Locale xxzzaaLocale = new Locale("xx", "zz", "aa");

    VarDefinition varDefinition = new VarDefinition(name, Object.class);

    varDefinition.setDescription(xxLocale, description);
    varDefinition.setDescription(xxLocale, xxDescription);
    varDefinition.setDescription(xxyyLocale, xxyyDescription);

    assertEquals(xxyyDescription, varDefinition.getDescription(xxyyaaLocale));
    assertEquals(xxDescription, varDefinition.getDescription(xxzzaaLocale));
  }
}

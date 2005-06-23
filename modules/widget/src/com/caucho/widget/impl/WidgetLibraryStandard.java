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

import com.caucho.widget.*;

import java.util.LinkedHashMap;

/**
 * A {@link WidgetLibrary} for the standard widgets included
 * in the namespace "http://caucho.com/ns/widget".
 */
public class WidgetLibraryStandard
  implements WidgetLibrary
{
  private LinkedHashMap<String, Class<? extends Widget>> _nameMap
    = new LinkedHashMap<String, Class<? extends Widget>>();

  public WidgetLibraryStandard()
  {
    put("widget", WidgetConfig.class);
    put("label", LabelWidget.class);
    put("textbox", TextboxWidget.class);
    put("box", BoxWidget.class);
  }

  private void put(String name, Class<? extends Widget> cl)
  {
    _nameMap.put(name, cl);
  }

  public String getNamespaceURI()
  {
    return "http://caucho.com/ns/widget";
  }

  /**
   * @param name the name of a widget in this namespace
   * @return null if no match for <i>name</i> in this namespace
   */
  public Class<? extends Widget> getWidgetClass(String name)
  {
    return _nameMap.get(name);
  }
}

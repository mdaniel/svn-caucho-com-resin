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

import com.caucho.widget.WidgetLibrary;
import com.caucho.widget.Widget;

import java.util.HashMap;

public class WidgetLibraries
{
  private HashMap<String, WidgetLibrary> _widgetLibraryMap;

  public void add(WidgetLibrary widgetLibrary)
  {
    if (_widgetLibraryMap == null)
    _widgetLibraryMap = new  HashMap<String, WidgetLibrary>();

    String namespaceURI = widgetLibrary.getNamespaceURI();

    if (_widgetLibraryMap.containsKey(namespaceURI))
      throw new IllegalArgumentException("duplicate namespace " + namespaceURI); // XXX: should merge

    _widgetLibraryMap.put(namespaceURI, widgetLibrary);
  }

  public Class<? extends Widget> getWidgetClass(String namespaceURI,
                                                String localName)
  {
    WidgetLibrary widgetLibrary = _widgetLibraryMap.get(namespaceURI);

    if (widgetLibrary == null)
      return null;

    return widgetLibrary.getWidgetClass(localName);
  }
}

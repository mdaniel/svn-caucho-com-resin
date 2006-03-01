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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.doclet;

import java.util.*;
import java.util.logging.*;

import com.caucho.log.Log;
import com.caucho.xml.XmlUtil;

/**
 * Represents a class.
 */
public class TagImpl {
  private static final Logger log = Log.open(TagImpl.class);

  private String _name;
  private String _text;

  private HashMap<String,String> _attr;

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void setText(String text)
  {
    _text = text;
  }

  public String getText()
  {
    return _text;
  }

  /**
   * Returns the named attributes.
   */
  public String getAttribute(String name)
  {
    parseAttributes();

    return _attr.get(name);
  }
  
  /**
   * Parses the tags attributes.
   */
  private void parseAttributes()
  {
    if (_attr != null)
      return;

    try {
      _attr = XmlUtil.splitNameList(_text);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}

/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jsp.cfg;

import java.util.ArrayList;

/**
 * Configuration for the taglib attribute in the .tld
 */
public class TldAttribute {
  private String _name;
  private boolean _required;
  private boolean _rtexprvalue;
  private Class _type = String.class;
  private String _description;
  private boolean _isFragment;

  /**
   * Sets the attribute name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the attribute name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets true if the attribute is required.
   */
  public void setRequired(boolean required)
  {
    _required = required;
  }

  /**
   * Returns true if the attribute is required.
   */
  public boolean getRequired()
  {
    return _required;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public void setRtexprvalue(boolean rtexprvalue)
  {
    _rtexprvalue = rtexprvalue;
  }

  /**
   * Returns true if runtime expressions are required.
   */
  public boolean getRtexprvalue()
  {
    return _rtexprvalue;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public void setFragment(boolean isFragment)
  {
    _isFragment = isFragment;
  }

  /**
   * Sets true if the attribute allows runtime expressions
   */
  public boolean isFragment()
  {
    return _isFragment;
  }

  /**
   * Sets the type of the attribute.
   */
  public void setType(Class type)
  {
    _type = type;
  }

  /**
   * Returns the type of the attribute.
   */
  public Class getType()
  {
    return _type;
  }
}

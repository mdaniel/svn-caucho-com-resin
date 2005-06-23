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

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class MockExternalContext
  implements ExternalContext
{
  private Map<String, Object> _attributeMap = new HashMap<String, Object>();

  public void setApplicationAttribute(String name, Object value)
  {
    _attributeMap.put(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) _attributeMap.get(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return Collections.enumeration(_attributeMap.keySet());
  }

  public void removeApplicationAttribute(String name)
  {
    _attributeMap.remove(name);
  }
}

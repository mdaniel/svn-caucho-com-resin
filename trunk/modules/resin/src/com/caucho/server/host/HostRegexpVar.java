/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.server.host;

import java.util.ArrayList;

/**
 * A configuration entry for a host
 */
class HostRegexpVar
{
  private String _name;
  private ArrayList<String> _regexp;

  HostRegexpVar(String name)
  {
    this(name, new ArrayList<String>());
  }

  HostRegexpVar(String name, ArrayList<String> regexp)
  {
    _name = name;
    _regexp = regexp;
  }
  
  public String getName()
  {
    return _name;
  }
  
  public String getHostName()
  {
    return _name;
  }
  
  public ArrayList<String> getRegexp()
  {
    // server/13t0
    return _regexp;
  }
  
  public ArrayList<String> getRegex()
  {
    return getRegexp();
  }
}

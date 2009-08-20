/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Emil Ong
 */

package com.caucho.server.admin;

import java.io.Serializable;

public class QueryTagsQuery implements Serializable {
  private String _staging;
  private String _host;
  private String _type;
  private String _name;

  public QueryTagsQuery()
  {
  }

  public QueryTagsQuery(String staging, String type, String host, String name)
  {
    _staging = staging;
    _type = type;
    _host = host;
    _name = name;
  }

  public String getStaging()
  {
    return _staging;
  }

  public void setStaging(String staging)
  {
    _staging = staging;
  }

  public String getType()
  {
    return _type;
  }

  public void setType(String type)
  {
    _type = type;
  }

  public String getHost()
  {
    return _host;
  }

  public void setHost(String host)
  {
    _host = host;
  }

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + 
      "[" + _staging + "/" + _type + "/" + _host + "/" + _name + "]";
  }
}

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
*/

package com.caucho.server.admin;

import com.caucho.json.Json;
import com.caucho.json.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ListJmxQueryReply extends ManagementQueryReply
{
  private List<Bean> _beans = new ArrayList<Bean>();

  public void add(Bean bean)
  {
    _beans.add(bean);
  }
  
  public List<Bean> getBeans() {
    return _beans;
  }

  public static class Bean implements Serializable
  {
    @Json(name = "name")
    private String _name;

    @Json(name = "attributes")
    private List<Attribute> _attributes; // = new ArrayList<Attribute>();

    @Json(name = "operations")
    private List<Operation> _operations; // = new ArrayList<Operation>();

    public String getName()
    {
      return _name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public void add(Attribute attr)
    {
      if (_attributes == null) {
        _attributes = new ArrayList<Attribute>();
      }
      
      _attributes.add(attr);
    }

    public void add(Operation op)
    {
      if (_operations == null) {
        _operations = new ArrayList<Operation>();
      }
      
      _operations.add(op);
    }

    public List<Attribute> getAttributes()
    {
      return _attributes;
    }

    public List<Operation> getOperations()
    {
      return _operations;
    }
  }

  public static class Attribute implements Serializable
  {
    @Json(name = "name")
    private String _name;

    @Transient
    private String _info;

    @Json(name = "value")
    private Object _value;

    public String getName()
    {
      return _name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public Object getValue()
    {
      return _value;
    }

    public void setValue(Object value)
    {
      _value = value;
    }

    public String getInfo()
    {
      return _info;
    }

    public void setInfo(String info)
    {
      _info = info;
    }
  }

  public static class Operation implements Serializable
  {
    @Json(name = "name")
    private String _name;

    @Json(name = "type")
    private String _type;

    @Json(name = "description")
    private String _description;

    @Json(name = "parameters")
    private List<Param> _params;

    public String getName()
    {
      return _name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public String getType()
    {
      return _type;
    }

    public void setType(String type)
    {
      _type = type;
    }

    public void add(Param par)
    {
      if (_params == null)
        _params = new ArrayList<Param>();

      _params.add(par);
    }

    public List<Param> getParams()
    {
      return _params;
    }

    public String getDescription()
    {
      return _description;
    }

    public void setDescription(String description)
    {
      _description = description;
    }

  }

  public static class Param implements Serializable
  {
    @Json(name = "name")
    private String _name;

    @Json(name = "type")
    private String _type;

    @Json(name = "description")
    private String _description;

    public String getName()
    {
      return _name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public String getType()
    {
      return _type;
    }

    public void setType(String type)
    {
      _type = type;
    }

    public void setDescription(String description)
    {
      _description = description;
    }

    public String getDescription()
    {
      return _description;
    }
  }
}

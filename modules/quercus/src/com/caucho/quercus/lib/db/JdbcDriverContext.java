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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import java.util.HashMap;

public class JdbcDriverContext
{
  private String _defaultDriver;
  private String _defaultUrlPrefix;
  private String _defaultEncoding;

  private HashMap<String,String> _protocolDriverMap
    = new HashMap<String,String>();

  public JdbcDriverContext()
  {
    _defaultDriver = "com.mysql.jdbc.Driver";
    _defaultUrlPrefix = "jdbc:mysql://";

    // php/144b, php/1464, php/1465
    _defaultEncoding = "ISO8859_1";

    _protocolDriverMap.put("mysql", _defaultDriver);
    _protocolDriverMap.put("sqlite", "org.sqlite.JDBC");
  }

  public String getDefaultDriver()
  {
    return _defaultDriver;
  }

  public String getDefaultUrlPrefix()
  {
    return _defaultUrlPrefix;
  }

  public String getDefaultEncoding()
  {
    return _defaultEncoding;
  }

  public String getDriver(String protocol)
  {
    return _protocolDriverMap.get(protocol);
  }

  public void setDefaultDriver(String driver)
  {
    _defaultDriver = driver;
  }

  public void setDefaultUrlPrefix(String prefix)
  {
    _defaultUrlPrefix = prefix;
  }

  public void setDefaultEncoding(String encoding)
  {
    _defaultEncoding = encoding;
  }

  public void setProtocol(String protocol, String driver)
  {
    _protocolDriverMap.put(protocol, driver);
  }
}

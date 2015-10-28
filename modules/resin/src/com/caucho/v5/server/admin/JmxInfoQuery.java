/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.admin;

public class JmxInfoQuery implements java.io.Serializable
{
  private String _name;

  private JmxInfoQuery()
  {
  }

  public JmxInfoQuery(String name)
  {
    _name = name;

    if (_name == null)
      throw new NullPointerException();
  }

  public String getName()
  {
    return _name;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

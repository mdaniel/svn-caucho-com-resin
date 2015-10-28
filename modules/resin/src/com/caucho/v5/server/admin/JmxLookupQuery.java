/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.admin;

public class JmxLookupQuery implements java.io.Serializable
{
  private String _pattern;

  private JmxLookupQuery()
  {
  }

  public JmxLookupQuery(String pattern)
  {
    _pattern = pattern;

    if (_pattern == null)
      throw new NullPointerException();
  }

  public String getPattern()
  {
    return _pattern;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _pattern + "]";
  }
}

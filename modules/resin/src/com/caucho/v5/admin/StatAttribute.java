/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 */
public class StatAttribute
{
  private final String _name;

  protected StatAttribute(String name)
  {
    _name = name;
  }

  /**
   * Returns the attribute's official name
   */
  public final String getName()
  {
    return _name;
  }

  /**
   * Polls the statistics attribute.
   */
  public long pollValue()
  {
    return 0;
  }

  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _name + "]");
  }
}

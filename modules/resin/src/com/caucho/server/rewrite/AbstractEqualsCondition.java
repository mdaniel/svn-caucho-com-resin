/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.util.L10N;
import com.caucho.config.ConfigException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A rewrite condition that passes if a named value from the request exactly
 * equals a specified value.
 */
abstract public class AbstractEqualsCondition
  extends AbstractCondition
{
  private static final L10N L =  new L10N(AbstractEqualsCondition.class);

  private String _value;
  private boolean _isIgnoreCase;

  public void setValue(String value)
  {
    _value = value;
  }

  /**
   * Set's the value to match against, required.
   * @return
   */
  public String getValue()
  {
    return _value;
  }

  /**
   * Set's the ignoreCase, if true the case is unimportant in the comparison,
   * default false.
   */
  public void setIgnoreCase(boolean ignoreCase)
  {
    _isIgnoreCase = ignoreCase;
  }

  public boolean isIgnoreCase()
  {
    return _isIgnoreCase;
  }

  @PostConstruct
  public void init()
  {
    required(_value, "value");
  }

  public boolean evaluate(RewriteContext rewriteContext)
  {
    String value = getValue(rewriteContext.getRequest());

    if (value == null)
      return false;

    if (_isIgnoreCase)
      return value.equalsIgnoreCase(_value);
    else
      return value.equals(_value);
  }

  /**
   * Returns the value, if it is null then the comparison always fails.
   */
  protected abstract String getValue(ServletRequest request);
}

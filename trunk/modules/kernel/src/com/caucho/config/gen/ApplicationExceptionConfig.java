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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.config.gen;

public class ApplicationExceptionConfig {
  private String _id;
  private Class<?> _exceptionClass;
  private String _rollback;
  private boolean _inherited;

  public ApplicationExceptionConfig()
  {
  }

  public Class<?> getExceptionClass()
  {
    return _exceptionClass;
  }

  public String getId()
  {
    return _id;
  }

  public String getRollback()
  {
    return _rollback;
  }

  public boolean isRollback()
  {
    if (_rollback == null)
      return false;

    if (_rollback.equals("true"))
      return true;

    return false;
  }

  public void setExceptionClass(Class<?> exceptionClass)
  {
    _exceptionClass = exceptionClass;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public void setRollback(String rollback)
  {
    _rollback = rollback;
  }

  public void setInherited(boolean inherited)
  {
    _inherited = inherited;
  }

  public boolean isInherited()
  {
    return _inherited;
  }
}

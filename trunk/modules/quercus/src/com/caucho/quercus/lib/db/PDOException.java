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
 * @author Sam
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.QuercusLanguageException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.Location;

public class PDOException
  extends QuercusLanguageException
{
  private final String _code;
  private final String _message;

  private Location _location;

  public PDOException(Env env, String code, String message)
  {
    super(env);

    _code = code;
    _message = "SQLSTATE[" + code + "]: " + message;

    _location = env.getLocation();
  }

  public String getCode()
  {
    return _code;
  }


  public Location getLocation(Env env)
  {
    return _location;
  }

  public String getMessage()
  {
    return _message;
  }

  public String getMessage(Env env)
  {
    return getMessage();
  }

  /**
   * Converts the exception to a Value.
   */
  @Override
  public Value toValue(Env env)
  {
    Value e = env.createException("PDOException", _code, _message);

    return e;
  }
}

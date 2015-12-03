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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PDOError
{
  private final static L10N L = new L10N(PDOError.class);
  private final static Logger log = Logger.getLogger(PDOError.class.getName());

  private static final String ERR_NONE = PDO.ERR_NONE;
  private static final String ERR_GENERAL = "HY000";

  private int _errmode;
  private boolean _isError;
  private String _errorCode = ERR_NONE;
  private ArrayValue _errorInfo;

  public PDOError()
  {
  }

  /**
   * Clear the error if there is one.
   */
  public void clear()
  {
    _isError = false;
    _errorCode = ERR_NONE;
    _errorInfo = null;
  }

  private void error(Env env,
                     String errorCode,
                     int driverError,
                     String errorMessage)
  {
    _isError = true;

    int level = Math.max(_errmode, PDO.ERRMODE_SILENT);

    _errorCode = errorCode;

    _errorInfo = new ArrayValueImpl();
    _errorInfo.put(errorCode);
    _errorInfo.put(driverError);
    _errorInfo.put(errorMessage);

    if (level == PDO.ERRMODE_WARNING) {
      env.warning("SQLSTATE[" + errorCode + "]: " + errorMessage);
    }
    else if (level == PDO.ERRMODE_EXCEPTION) {
      throw new PDOException(env, errorCode, errorMessage);
    }
  }
  /**
   * Save an error for subsequent calls to
   * {@link #errorCode} and {@link #errorInfo},
   * and depending on the value of {@link #setErrmode}
   * show nothing, show a warning, or throw an exception.
   */
  public void error(Env env, Throwable exception)
  {
    log.log(Level.FINE, exception.toString(), exception);

    String errorCode;
    String errorMessage;
    int driverError;

    if (exception instanceof SQLException) {
      SQLException sqlException = (SQLException) exception;
      errorCode = sqlException.getSQLState();
      errorMessage = sqlException.getMessage();
      driverError = sqlException.getErrorCode();
    }
    else {
      errorCode = ERR_GENERAL;
      errorMessage = exception.getMessage();
      driverError = 0;
    }

    error(env, errorCode, driverError, errorMessage);
  }

  public String getErrorCode()
  {
    return _errorCode;
  }

  public ArrayValue getErrorInfo()
  {
    if (_errorInfo == null) {
      _errorInfo = new ArrayValueImpl();
      _errorInfo.put(ERR_NONE);
    }

    return _errorInfo;
  }

  public int getErrmode()
  {
    return _errmode;
  }

  public boolean isError()
  {
    return _isError;
  }

  /**
   * Show a notice and return a "HY000" general error for subsequent calls to
   * {@link #errorCode} and {@link #errorInfo}.
   */
  public void notice(Env env, String message)
  {
    _isError = true;

    _errorCode = ERR_GENERAL;

    _errorInfo = new ArrayValueImpl();
    _errorInfo.put(_errorCode);
    _errorInfo.put(2050);
    _errorInfo.put("");

    env.notice(message);
  }

  /**
   * Set's the error mode.
   *
   * <dl>
   * <dt>{@link ERRMODE_SILENT}
   * <dt>{@link ERRMODE_WARNING}
   * <dt>{@link ERRMODE_EXCEPTION}
   * </dl>
   *
   * @return true on success, false on error.
   */
  public boolean setErrmode(Env env, int value)
  {
    switch (value) {
      case PDO.ERRMODE_SILENT:
      case PDO.ERRMODE_WARNING:
      case PDO.ERRMODE_EXCEPTION:
        _errmode = value;
        return true;

      default:
        warning(env, L.l("invalid error mode"));
        return false;
    }
  }

  /**
   * Show a warning and return a "HY000" general error for subsequent calls to
   * {@link #errorCode} and {@link #errorInfo}.
   */
  public void warning(Env env, String message)
  {
    _isError = true;

    _errorCode = ERR_GENERAL;

    _errorInfo = new ArrayValueImpl();
    _errorInfo.put(_errorCode);

    if (_errmode == PDO.ERRMODE_EXCEPTION) {
      throw new PDOException(env, _errorCode, message);
    }
    else {
      env.warning("SQLSTATE[" + _errorCode + "]: " + message);
    }
  }

  public void unsupportedAttribute(Env env, int attribute)
  {
    error(env, "IM001", 0, L.l("attribute `{0}' is not supported", attribute));
  }

  public void unsupportedAttributeValue(Env env, Object value)
  {
    error(env, "IM001", 0, L.l("attribute value `{0}' is not supported", value));
  }
}

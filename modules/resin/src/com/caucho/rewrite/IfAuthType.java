/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;

/**
* A rewrite condition that passes if the auth-type is exactly
* equal to the specified value.
 * Valid auth types are  BASIC, CLIENT-CERT, DIGEST, FORM.
*/
public class IfAuthType implements RequestPredicate
{
  private static final L10N L = new L10N(IfAuthType.class);

  private String _authType;

  public void setValue(String authType)
  {
    if ("NONE".equalsIgnoreCase(authType))
      _authType = null;
    else if (HttpServletRequest.BASIC_AUTH.equalsIgnoreCase(authType))
      _authType = HttpServletRequest.BASIC_AUTH;
    else if (HttpServletRequest.CLIENT_CERT_AUTH.equalsIgnoreCase(authType))
      _authType = HttpServletRequest.CLIENT_CERT_AUTH;
    else if (HttpServletRequest.DIGEST_AUTH.equalsIgnoreCase(authType))
      _authType = HttpServletRequest.DIGEST_AUTH;
    else if (HttpServletRequest.FORM_AUTH.equalsIgnoreCase(authType))
      _authType = HttpServletRequest.FORM_AUTH;
    else
      throw new ConfigException(L.l("auth-type expects a 'value' of BASIC, CLIENT-CERT, DIGEST, FORM, or NONE"));
  }

  public boolean isMatch(HttpServletRequest request)
  {
    String authType = request.getAuthType();

    if (authType == null)
      authType = "none";

    if (_authType != null)
      return _authType.equalsIgnoreCase(authType);
    else
      return "none".equalsIgnoreCase(authType);
  }
}

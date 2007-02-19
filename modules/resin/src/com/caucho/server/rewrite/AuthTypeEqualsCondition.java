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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

/**
* A rewrite condition that passes if the auth-type is exactly
* equal to the specified value.
 * Valid auth types are  BASIC, CLIENT-CERT, DIGEST, FORM.
*/
public class AuthTypeEqualsCondition
 extends AbstractEqualsCondition
{
  private static final L10N L = new L10N(AuthTypeEqualsCondition.class);

  public String getTagName()
  {
    return "auth-type-equals";
  }

  @PostConstruct
  public void init()
  {
    String value = getValue();

    if (value.equals("BASIC")) {
    }
    else if (value.equals("CLIENT-CERT")) {
    }
    else if (value.equals("DIGEST")) {
    }
    else if (value.equals("FORM")) {
    }
    else {
      throw new ConfigException(L.l("auth-type expects a 'value' of BASIC, CLIENT-CERT, DIGEST, or FORM"));
    }
  }

  protected String getValue(HttpServletRequest request)
  {
    return request.getAuthType();
  }
}

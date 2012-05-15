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
 * @author Scott Ferguson
 */

package com.caucho.quercus.servlet;

import javax.servlet.ServletException;

/**
 * Servlet to call PHP through javax.script.
 */
@SuppressWarnings("serial")
public class GoogleQuercusServlet extends QuercusServlet
{
  private String _gsBucket;

  public GoogleQuercusServlet()
  {
  }

  @Override
  protected QuercusServletImpl getQuercusServlet(boolean isResin)
  {
    QuercusServletImpl impl = new ProGoogleQuercusServlet(_gsBucket);

    return impl;
  }

  /**
   * Sets a named init-param to the passed value.
   *
   * @throws ServletException if the init-param is not recognized
   */
  protected void setInitParam(String paramName, String paramValue)
    throws ServletException
  {
    if ("gs-bucket".equals(paramName)) {
      _gsBucket = paramValue;
    }
    else {
      super.setInitParam(paramName, paramValue);
    }
  }
}


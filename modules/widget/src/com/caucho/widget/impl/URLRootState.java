/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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

package com.caucho.widget.impl;

import com.caucho.widget.*;
import com.caucho.util.L10N;

import java.util.LinkedHashMap;
import java.util.Map;

public class URLRootState
  extends URLState
{
  private static final L10N L = new L10N(URLState.class);

  private ExternalConnection _externalConnection;
  private String _submitPrefix;

  private LinkedHashMap<String, String[]> _parameterMap
    = new LinkedHashMap<String, String[]>();

  private boolean _isSecure;
  private boolean _isSecureSet;
  private String _pathInfo;

  public void setExternalConnection(ExternalConnection externalConnection)
  {
    _externalConnection = externalConnection;
  }

  public void setSubmitPrefix(String submitPrefix)
  {
    _submitPrefix = submitPrefix;
  }

  public void init()
  {
    super.init();

    if (_externalConnection == null)
      throw new IllegalStateException(L.l("`{0}' is required", "external-connection"));
  }

  public void destroy()
  {
    try {
      _parameterMap.clear();
    }
    finally {
      _externalConnection = null;

      _isSecure = false;
      _isSecureSet = false;
      _pathInfo = null;
      _submitPrefix = null;

      super.destroy();
    }
  }

  public Map<String, String[]> getBackingMap()
  {
    return _parameterMap;
  }

  public boolean isSecure()
  {
    return _isSecureSet ? _isSecure : _externalConnection.isSecure();
  }

  public void setSecure(boolean isSecure)
    throws WidgetSecurityException
  {
    if (_isSecureSet == true) {
      if (isSecure)
        _isSecure = true;
    }
    else {
      _isSecureSet = true;
      _isSecure = isSecure;
    }
  }

  public void setPathInfo(String pathInfo)
  {
    if (_pathInfo != null)
      throw new UnsupportedOperationException(L.l("`{0}' already set", "path-info"));

    _pathInfo = pathInfo;
  }

  public String createURLString()
    throws WidgetException
  {
    getStateWalker().doURL();

    String url;

    if (_isSecureSet) {
      if (_submitPrefix != null)
        url = _externalConnection.createSubmitURL(_submitPrefix, _pathInfo, _parameterMap, _isSecure);
      else
        url = _externalConnection.createURL(_pathInfo, _parameterMap, _isSecure);
    }
    else {
      if (_submitPrefix != null)
        url = _externalConnection.createSubmitURL(_submitPrefix, _pathInfo, _parameterMap);
      else
        url = _externalConnection.createURL(_pathInfo, _parameterMap);
    }

    return url;
  }
}

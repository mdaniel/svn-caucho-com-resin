/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.ProGoogleQuercus;
import com.caucho.quercus.QuercusContext;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProGoogleQuercusServlet extends GoogleQuercusServletImpl
{
  private final String _gsBucket;

  public ProGoogleQuercusServlet(String gsBucket)
  {
    _gsBucket = gsBucket;
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new ProGoogleQuercus();

      if (_gsBucket != null) {
        _quercus.setIni("google.cloud_storage_bucket", _gsBucket);
      }
    }

    return _quercus;
  }
}

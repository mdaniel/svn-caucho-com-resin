/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.ProGoogleQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.BooleanValue;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProGoogleQuercusServlet extends GoogleQuercusServletImpl
{
  private final String _gsBucket;
  private final boolean _isGsEnabled;

  public ProGoogleQuercusServlet()
  {
    this(null, true);
  }

  public ProGoogleQuercusServlet(String gsBucket)
  {
    this(gsBucket, true);
  }

  public ProGoogleQuercusServlet(String gsBucket, boolean isGsEnabled)
  {
    _gsBucket = gsBucket;
    _isGsEnabled = isGsEnabled;
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new ProGoogleQuercus();

      _quercus.setIni("quercus.gs_enabled", BooleanValue.create(_isGsEnabled));

      if (_gsBucket != null) {
        _quercus.setIni("quercus.gs_bucket", _gsBucket);
      }
    }

    return _quercus;
  }
}

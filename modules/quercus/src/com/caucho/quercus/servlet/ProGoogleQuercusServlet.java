/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.ProGoogleQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProGoogleQuercusServlet extends GoogleQuercusServletImpl
{
  public ProGoogleQuercusServlet()
  {
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new ProGoogleQuercus();
    }

    return _quercus;
  }
}

/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.ProQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProQuercusServlet extends QuercusServletImpl
{
  private static final L10N L = new L10N(ProQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ProQuercusServlet.class.getName());

  public ProQuercusServlet()
  {
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null)
      _quercus = new ProQuercus();

    return _quercus;
  }
}


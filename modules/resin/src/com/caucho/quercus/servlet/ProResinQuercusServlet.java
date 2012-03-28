/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.ProResinQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ProResinQuercusServlet extends ResinQuercusServlet
{
  private static final L10N L = new L10N(ProResinQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ProResinQuercusServlet.class.getName());
  
  public ProResinQuercusServlet()
  {
  }

  @Override
  public void setProfileProbability(double probability)
  {
    getQuercus().setProfileProbability(probability);
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new ProResinQuercus();
    }

    return _quercus;
  }
}


/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.check;

import io.baratine.service.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.env.health.ExprHealthCheckImpl;

/**
 * Evaluates user supplied JSP EL expressions as a boolean. 
 * <p>
 * Resulting status depends on the category of expression that evaluates to false.
 * 
 */
@Named
@Startup
@Singleton
@Configurable
public class ExprHealthCheck extends ExprHealthCheckImpl
{
  /**
   * Add JSP-EL expression that will result in FATAL health status
   */
  @Configurable
  public void addFatalTest(ExprCfg test)
  {
    super.addFatalTest(test);
  }
  
  /**
   * Add JSP-EL expression that will result in CRITICAL health status
   */
  @Configurable
  public void addCriticalTest(ExprCfg test)
  {
    super.addCriticalTest(test);
  }
  
  /**
   * Add JSP-EL expression that will result in WARNING health status
   */
  @Configurable
  public void addWarningTest(ExprCfg test)
  {
    super.addWarningTest(test);
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;

/**
 * Qualifies an action to execute based on the evaluation of an JSP EL 
 * expression. Expression can include references to system properties, config 
 * properties, and JMX mbean attributes.
 * <p>
 * <pre>{@code
 * <health:DumpHeap>
 *   <health:IfExpr test="${resin.professional}"/>
 * </health:DumpHeap>
 * 
 * <health:DumpHeap>
 *   <health:IfExpr test="${mbean('java.lang:type=OperatingSystem').AvailableProcessors  >= 1}"/>
 * </health:DumpHeap>
 * 
 * <health:DumpHeap>
 *   <health:IfExpr test="${user.timezone eq 'PST'}"/>
 * </health:DumpHeap>
 * }</pre>
 *
 */
@Startup
@Configurable
public class IfExpr extends HealthPredicateScheduledBase
{
  private static final L10N L = new L10N(IfExpr.class);

  private ExprCfg _test;
  
  @PostConstruct
  public void init()
  {
    if (_test == null)
      throw new ConfigException(L.l("<health:{0}> requires 'test' attribute",
                                    getClass().getSimpleName()));
  }
  
  public ExprCfg getTest()
  {
    return _test;
  }

  /**
   * Sets the JSP-EL expression value
   */
  @Configurable
  public void setTest(ExprCfg test)
  {
    _test = test;
  }

  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent)) {
      return false;
    }
    
      //return _test.evalBoolean(ConfigELContext.EL_CONTEXT);
    return _test.evalBoolean(ConfigContext.getEnvironment());
  }
}

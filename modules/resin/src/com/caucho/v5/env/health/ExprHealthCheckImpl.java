/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.env.health;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.health.check.AbstractHealthCheck;

public class ExprHealthCheckImpl extends AbstractHealthCheck
{
  private List<ExprCfg> _fatalTests = new ArrayList<ExprCfg>();
  private List<ExprCfg> _criticalTests = new ArrayList<ExprCfg>();
  private List<ExprCfg> _warningTests = new ArrayList<ExprCfg>();
  
  @Override
  public HealthCheckResult checkHealth()
  {
    try {
      Function<String,Object> env = Config.getEnvironment();
      
      for (ExprCfg test : _fatalTests) {
        if (test.evalBoolean(env)) {
          return new HealthCheckResult(HealthStatus.FATAL);
        }
      }
      
      for (ExprCfg test : _criticalTests) {
        if (test.evalBoolean(env)) {
          return new HealthCheckResult(HealthStatus.CRITICAL);
        }
      }

      for (ExprCfg test : _warningTests) {
        if (test.evalBoolean(env)) {
          return new HealthCheckResult(HealthStatus.WARNING);
        }
      }

    } catch (Exception e) {
      HealthCheckResult result = new HealthCheckResult(HealthStatus.UNKNOWN);
      result.setMessage(e.getMessage());
      result.setException(e);
      return result;
    }
    
    return new HealthCheckResult(HealthStatus.OK);
  }
  
  public void addFatalTest(ExprCfg test)
  {
    _fatalTests.add(test);
  }
  
  public List<ExprCfg>getFatalTests()
  {
    return _fatalTests;
  }
  
  public void addCriticalTest(ExprCfg test)
  {
    _criticalTests.add(test);
  }
  
  public List<ExprCfg>getCriticalTests()
  {
    return _criticalTests;
  }

  public void addWarningTest(ExprCfg test)
  {
    _warningTests.add(test);
  }
  
  public List<ExprCfg>getWarningTests()
  {
    return _warningTests;
  }
}

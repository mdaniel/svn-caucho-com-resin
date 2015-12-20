/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.action.Restart;
import com.caucho.v5.health.predicate.IfHealthFatal;

public class BasicHealthChecks
{
  public BasicHealthChecks()
  {
  }
  
  public void init()
  {
    initStandardHealthChecks();
    initDefaultActions();
  }
  
  public void initStandardHealthChecks()
  {
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    if (healthService == null) {
      return;
    }
    
    if (! healthService.containsHealthCheck(JvmDeadlockHealthCheck.class)) {
      JvmDeadlockHealthCheck deadlockHealthCheck 
        = new JvmDeadlockHealthCheck();
      deadlockHealthCheck.init();
    }
    
    if (! healthService.containsHealthCheck(MemoryTenuredHealthCheck.class)) {
      MemoryTenuredHealthCheck tenuredHealthCheck
        = new MemoryTenuredHealthCheck();
      tenuredHealthCheck.init();
    }
    
    /*
    if (! healthService.containsHealthCheck(MemoryPermGenHealthCheck.class)) {
      MemoryPermGenHealthCheck permGenHealthCheck
        = new MemoryPermGenHealthCheck();
      permGenHealthCheck.init();
    }
    */

    if (! healthService.containsHealthCheck(CpuHealthCheck.class)) {
      CpuHealthCheck cpuHealthCheck
        = new CpuHealthCheck();
      cpuHealthCheck.init();
    }
    
    /*
    if (! healthService.containsHealthCheck(HeartbeatHealthCheck.class)) {
      HeartbeatHealthCheck heartbeatHealthCheck
        = new HeartbeatHealthCheck();
      heartbeatHealthCheck.init();
    }
    */
    
    /*
    if (! healthService.containsHealthCheck(TransactionHealthCheck.class)) {
      TransactionHealthCheck transactionHealthCheck
        = new TransactionHealthCheck();
      transactionHealthCheck.init();
    }
    */
    
    if (! healthService.containsHealthCheck(HealthSystemHealthCheck.class)) {
      HealthSystemHealthCheck healthServiceHealthCheck
        = new HealthSystemHealthCheck();
      healthServiceHealthCheck.init();
    }
  }
    
  public void initDefaultActions()
  {
    Restart restart = new Restart();
    restart.add(new IfHealthFatal());
    restart.init();
  }
}

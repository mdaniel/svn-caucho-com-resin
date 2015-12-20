/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.analysis;

import com.caucho.v5.env.health.HealthStatus;

public interface HealthAnalyzer
{
  public String getName();
  
  public void start();
  
  public HealthStatus analyze();
  
  public String getLastMessage();
}

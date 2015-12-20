/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import com.caucho.v5.config.InlineConfig;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.profile.Profile;

/**
 * Configuration to configure the cpu profiler in the background.
 */
@InlineConfig
public class ProfileCpu
{
  public void setBackgroundPeriod(Period period)
  {
    Profile.create().setBackgroundPeriod(period.getPeriod());
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.env.meter.MeterBase;
import com.caucho.v5.env.meter.SampleMetadataAware;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.util.CurrentTime;

/**
 * A statistical meter for the health service.
 */
public class HealthMeter extends MeterBase implements SampleMetadataAware
{
  private StatSystem _statSystem;

  private long _sampleId;
  private String _sampleName;
  
  private HealthStatus _lastStatus = HealthStatus.UNKNOWN;
  
  public HealthMeter(String name)
  {
    super(name);
    
    _statSystem = StatSystem.getCurrent();
  }

  @Override
  public void setSampleMetadata(long id, String name)
  {
    _sampleId = id;
    _sampleName = name;
  }
  
  public long getSampleId()
  {
    return _sampleId;
  }
  
  public String getSampleName()
  {
    return _sampleName;
  }
  
  @Override
  public void sample()
  {
    
  }
  
  @Override
  public double calculate()
  {
    return _lastStatus.ordinal();
  }
  
  public void updateStatus(HealthStatus healthStatus)
  {
    _lastStatus = healthStatus;

    if (_statSystem != null && _sampleId != 0) {
      _statSystem.addSample(CurrentTime.getCurrentTime(),
                            _sampleId, 
                            healthStatus.ordinal());
    }
  }
}

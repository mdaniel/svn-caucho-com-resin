/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.analysis;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.env.health.HealthStatus;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.health.HealthSystemFacade;
import com.caucho.v5.health.meter.Meter;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.health.stat.StatServiceLocalImpl;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.util.L10N;

@Startup
public class AnomalyAnalyzer implements HealthAnalyzer
{
  private static final Logger log
    = Logger.getLogger(AnomalyAnalyzer.class.getName());
  private static final L10N L = new L10N(AnomalyAnalyzer.class);
  
  public static final String LOG_TYPE = "Caucho|Health|Anomaly";
  
  private LogSystem _logSystem;
  private String _logType;
  
  private String _name;
  private String _meterName;
  private Meter _meter;
  
  private double _minValue = Double.MIN_VALUE;
  private double _maxValue = Double.MAX_VALUE;
  
  private String _healthEventName;
  
  private long _minSamples = 60;
  private double _sigmaThreshold = 5.0;
  private double _minDeviation = 0;
  
  private double _minThreshold = Double.MAX_VALUE;
  private double _maxThreshold = Double.MIN_VALUE;
  
  private HealthStatus _lastStatus = HealthStatus.OK;
  
  private long _i;
  private double _xi; // running mean
  private double _qi; // running variance 
  
  private double _sample;
  
  @Configurable
  public void setMeterName(String name)
  {
    _meterName = name;
  }
  
  @ConfigArg(0)
  public void setMeter(String name)
  {
    _meterName = name;
  }
  
  @Configurable
  public void setMeterBean(Meter meter)
  {
    _meter = meter;
  }
  
  public Meter getMeter()
  {
    return _meter;
  }
  
  @Override
  public String getName()
  {
    return _name;
  }
  
  public void setName(String name)
  {
    _name = name;
  }
  
  @Configurable
  public void setHealthEvent(String name)
  {
    _healthEventName = name;
  }
  
  @Configurable
  public void setMinSamples(long minSamples)
  {
    _minSamples = minSamples;
  }
  
  public long getMinSamples()
  {
    return _minSamples;
  }
  
  @Configurable
  public void setMinValue(double value)
  {
    _minValue = value;
  }
  
  public double getMinValue()
  {
    return _minValue;
  }
  
  @Configurable
  public void setMaxValue(double value)
  {
    _maxValue = value;
  }
  
  public double getMaxValue()
  {
    return _maxValue;
  }
  
  @Configurable
  public void setSigmaThreshold(double sigmaThreshold)
  {
    _sigmaThreshold = sigmaThreshold;
  }
  
  public double getSigmaThreshold()
  {
    return _sigmaThreshold;
  }

  public double getMinDeviation()
  {
    return _minDeviation;
  }

  public void setMinDeviation(double minDeviation)
  {
    _minDeviation = minDeviation;
  }
  
  @Configurable
  public void setMinThreshold(double value)
  {
    _minThreshold = value;
  }
  
  @Configurable
  public double getMinThreshold()
  {
    return _minThreshold;
  }
  
  @Configurable
  public void setMaxThreshold(double value)
  {
    _maxThreshold = value;
  }
  
  @Configurable
  public double getMaxThreshold()
  {
    return _maxThreshold;
  }

  @PostConstruct
  public void init()
  {
    MeterService meterSystem = MeterService.getCurrent();
    
    if (meterSystem == null) {
      log.warning(L.l("{0} requires an active {1}",
                      this, MeterService.class.getName()));
      return;
    }
    
    StatSystem statSystem = StatSystem.getCurrent();
    
    if (statSystem == null) {
      log.warning(L.l("{0} requires an active {1}",
                      this, StatSystem.class.getName()));
      return;
    }
    
    statSystem.addAnalyzer(this);
  }
  
  @Override
  public void start()
  {
    Meter meter = findMeter();

    if (meter == null)
      log.finer(L.l("'{0}' is an unknown meter", _meterName));
    
    if (_name == null)
      _name = _meterName;
  }
  
  private Meter findMeter()
  {
    if (_meter == null) {
      String name = _meterName;
      
      StatSystem statSystem = StatSystem.getCurrent();
      
      if (statSystem != null)
        _meter = statSystem.getMeter(name);
    }
    
    return _meter;
  }
  
  @Override
  public HealthStatus analyze()
  {
    HealthStatus lastStatus = _lastStatus;
    
    HealthStatus status = analyzeImpl();
    
    if (status == HealthStatus.OK) { 
      if (log.isLoggable(Level.FINEST)) {
        log.finest(getClass().getSimpleName() + " " + getLastMessage());
      }
    }
    else if (status == lastStatus) {
      if (log.isLoggable(Level.FINER)) {
        log.finest(getClass().getSimpleName() + " " + getLastMessage());
      }
    }
    else {
      String message = getLastMessage(); 
      
      log.warning(getClass().getSimpleName() + " " + message);
      logAnomalyEvent(message);
      
      if (_healthEventName != null) {
        HealthSystemFacade.fireEvent(_healthEventName,
                                     message);
      }
    }
    
    return status;
  }
  
  protected HealthStatus analyzeImpl() 
  {
    analyzeSample();
    
    double i = _i;
    double xi = _xi;
    double qi = _qi;
    
    double sample = _sample;
    
    double std = 0;
    
    if (i > 1) {
      std = Math.sqrt(qi / (i - 1));
    }
    
    double deviation = xi - sample;
    
    if (deviation < 0) {
      deviation = -deviation;
    }
    
    double sigma = std > 0 ? deviation / std : 0;

    HealthStatus healthStatus = null;

    healthStatus = HealthStatus.OK;
    
    if (_i < getMinSamples()) {
    }
    else if (sigma < getSigmaThreshold()) {
    }
    else if (deviation < _minDeviation) {
    }
    else if (sample < _minValue) {
    }
    else if (_maxValue < sample) {
    }
    else if (sample <= _minThreshold) {
      healthStatus = HealthStatus.WARNING;
    }
    else if (_maxThreshold <= sample) {
      healthStatus = HealthStatus.WARNING;
    }
    else {
      healthStatus = HealthStatus.WARNING;
    }

    _lastStatus = healthStatus;

    return _lastStatus;
  }
  
  private double analyzeSample()
  {
    Meter meter = findMeter();
    
    if (meter == null)
      return 0;
    
    double sample = meter.calculate();
  
    long i = _i + 1;
  
    double xim1 = _xi;
  
    double xi = xim1 + (sample - xim1) / i;
  
    double qim1 = _qi;
  
    double qi = qim1 + (sample - xim1) * (sample - xi);
  
    _i = i;
    _xi = xi;
    _qi = qi;
    _sample = sample;
    
    return sample;
  }
  
  @Override
  public String getLastMessage()
  {
    double i = _i;
    double xi = _xi;
    double qi = _qi;
    
    double sample = _sample;
    
    double std = 0;
    
    if (i > 1)
      std = Math.sqrt(qi / (i - 1));
    
    double deviation = xi - sample;
    
    if (deviation < 0)
      deviation = -deviation;
    
    double sigma = std > 0 ? deviation / std : 0;
    
    String name = getName();
    
    if (name == null)
      name = _meterName;
    
    if (name == null) {
      name = String.valueOf(_meter);
    }
    
    return (name + " " + _lastStatus
            + "\n " + String.format("%.3f", sample)
            + " sample is " + String.format("%.2f%%", sample * 100 / xi)
            + " of " + String.format("%.3f", xi) + " avg"
            + ", " + String.format("%.3f", sigma) + " std deviations"
            + " (std=" + String.format("%.3f", std)
            + ", n=" + i +")");
  }
  
  private void logAnomalyEvent(String message)
  {
    if (_logSystem == null) {
      _logSystem = LogSystem.getCurrent();
      
      if (_logSystem != null) {
        _logType = _logSystem.createFullType(LOG_TYPE);
      }
    }
    
    if (_logSystem != null) {
      String eventName = _healthEventName;
      
      if (eventName == null) {
        eventName = "caucho.anomaly";
      }
      
      _logSystem.log(_logType, 
                     eventName,
                     Level.INFO, 
                     message);
        
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + '[' + _meter + ']';
  }
}

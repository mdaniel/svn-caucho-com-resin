/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthMeter;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.check.SystemHealthCheck;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Qualifies an action to match only when the health check status is flapping.
 * <p>
 * Flapping occurs when a health check changes state too frequently.  This can 
 * serve as convenient filter for nuisance actions, as well as a useful 
 * indicator of real issues that would otherwise be hard to detect. 
 * <p>
 * IfFlapping scans recent historical data for state changes to calculate a  
 * weighted percent change.  By default, recent state changes are weighted more 
 * than older changes.  This predicate matches if the overall weighted percent 
 * change exceeds a threshold.
 * <p>
 * Note: Use &lt;health:Not&gt; predicate for filtering.
 * 
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:Not>
 *    <health:IfFlapping healthCheck="${httpStatusHealthCheck}"/>
 *  </health:Not>
 * </health:Restart> 
 * }</pre>
 *
 */
@Startup
@Configurable
public class IfFlapping extends HealthPredicateCheckBase
{
  private static final L10N L = new L10N(IfFlapping.class);

  private StatSystem _statSystem;
  
  private int _sampleSize = 21;
  
  private double _lowWeight = 0.75D;
  private double _highWeight = 1.25D;

  private double _threshold = 0.5D;
  
  public IfFlapping()
  {
    super();
  }
  
  public IfFlapping(HealthCheck healthCheck)
  {
    super(healthCheck);
  }
  
  @PostConstruct
  public void init()
  {
    _statSystem = StatSystem.getCurrent();

    if (_statSystem == null)
      throw new ConfigException(L.l("<health:{0}> requires <resin:StatService> or <resin:AdminServices>",
                                    getClass().getSimpleName()));
  }

  /**
   * Returns the minimum sample size before attempting to detect flapping.
   */
  public int getSampleSize()
  {
    return _sampleSize;
  }

  /**
   * The minimum number of samples required before attempting to detect 
   * flapping.  Assuming the default health system check period of 5 minutes, 
   * a minimum of 1 hour and 45 minutes of data collection is required.
   * @param sampleSize minimum number of samples, default 21
   */
  @Configurable
  public void setSampleSize(int sampleSize)
  {
    _sampleSize = sampleSize;
  }

  /**
   * Returns the weighted percent change that must be exceeded to detect 
   * flapping, as a floating point number between 0 and 1.
   */
  public double getThreshold()
  {
    return _threshold;
  }

  /**
   * Flapping is detected when the calculated weighted percent change exceeds 
   * this threshold, 
   * @param threshold percent change threshold as a floating point number 
   * between 0 and 1, default 0.5 
   * (approximate 50% of samples, adjusted depending on weight)
   */
  @Configurable
  public void setThreshold(double threshold)
  {
    _threshold = threshold;
  }

  /**
   * Returns the low weight (least recent)
   */
  public double getLowWeight()
  {
    return _lowWeight;
  }

  /**
   * Older state changes can be weighted less than more recent changes.
   * 
   * @param lowWeight The starting weight (least recent), default .75
   */
  @Configurable
  public void setLowWeight(double lowWeight)
  {
    _lowWeight = lowWeight;
  }

  /**
   * Returns the high weight (most recent)
   */
  public double getHighWeight()
  {
    return _highWeight;
  }

  /**
   * Newer state changes can be weighted more than older changes.
   * 
   * @param highWeight The ending weight, default 1.25
   */
  @Configurable
  public void setHighWeight(double highWeight)
  {
    _highWeight = highWeight;
  }
  
  /**
   * Match if calculated weighted percent change exceeds the threshold
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    HealthCheck healthCheck = getHealthCheck();
    if (healthCheck == null)
      healthCheck = healthService.getHealthCheck(SystemHealthCheck.class);
    
    if (healthCheck == null)
      return false;
    
    HealthMeter meter = healthService.getHealthMeter(healthCheck);
    if (meter == null)
      return false;
    
    return isFlapping(healthService, healthCheck, meter.getSampleId());
  }
  
  protected boolean isFlapping(HealthSubSystem healthService, HealthCheck healthCheck, long sampleId)
  {
    // first we need to get the last n minutes of data, 
    // where n = sampleSize * health check period
    
    long healthCheckPeriod = healthService.getPeriod();
    long samplePeriod = _sampleSize * healthCheckPeriod;
    
    long endTime = CurrentTime.getCurrentTime();
    long beginTime = endTime - samplePeriod;
    
    StatServiceValue []data = _statSystem.getStatisticsData(sampleId,
                                                             beginTime, 
                                                             endTime, 
                                                             1);
    
    // we can't make a reliable determination about flapping 
    // unless we have at least the minimum sample size
    if (data == null || data.length < _sampleSize)
      return false;
    
    double percentChange = calculatePercentChange(data, _lowWeight, _highWeight);
    
    return percentChange > _threshold;
  }
  
  public static double calculatePercentChange(StatServiceValue []data, 
                                              double lowWeight, 
                                              double highWeight)
  {
    // walk through the data from oldest to newest
    // looking for state transitions...
    // older transitions are weighted less than more recent
    
    // the first datapoint is the initializer and is not used in calculations

    int sampleSize = (data.length - 1);
    
    double weightedTotal = 0;
    
    double lastState = data[0].getValue();
    
    double startTime = data[1].getTime();
    double endTime = data[data.length-1].getTime();
    
    for(int i=1; i<data.length; i++) {
      
      double currentState = data[i].getValue();
      
      if (currentState != lastState) {
        
        double currentTime = data[i].getTime();
        double percentTime = (currentTime - startTime) / (endTime - startTime);
        double weight = percentTime * (highWeight - lowWeight) + lowWeight;
        weightedTotal += weight;
        
        lastState = currentState;
      }
    }
    
    return (weightedTotal / sampleSize);
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import io.baratine.service.Startup;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HttpStatusHealthCheckImpl;

/**
 * Monitors one or more URLs on the current Resin instance by making an HTTP 
 * GET request and comparing the returned HTTP status code to a pattern.
 * <p>
 * Generates CRITICAL if the HTTP GET request failed to connect or the status 
 * code does not match the regexp.
 * 
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:Restart>
 *
 */
@Named
@Startup
@Singleton
@Configurable
public class HttpStatusHealthCheck extends HttpStatusHealthCheckImpl
{
  @PostConstruct
  public void init()
  {
    super.init();
  } 
  
  /**
   * Sets the server's ping host
   */
  @Override
  @Configurable
  public void setPingHost(String pingHost)
  {
    super.setPingHost(pingHost);
  }
  
  /**
   * Sets the server's ping port (default 80)
   */
  @Override
  @Configurable
  public void setPingPort(int pingPort)
  {
    super.setPingPort(pingPort);
  }
  
  /**
   * Adds a new URL to be tested.
   */
  @Override
  @Configurable
  public void addUrl(String url)
  {
    super.addUrl(url);
  }
  
  /**
   * Adds a new URL to be tested.
   */
  @Override
  @Configurable
  public void addUrlList(String url)
  {
    super.addUrlList(url);
  }
  
  /**
   * Sets the socket connection timeout (default 10 seconds) 
   */
  @Configurable
  public void setSocketTimeout(Period period)
  {
    super.setSocketTimeout(period.getPeriod());
  }
  
  /**
   * Set the HTTP status regular expression (default "200") 
   */
  @Override
  @Configurable
  public void setRegexp(Pattern regexp)
  {
    super.setRegexp(regexp);
  }

  @Override
  @Configurable
  public void setLogPeriod(Period period)
  {
    super.setLogPeriod(period);
  }
}

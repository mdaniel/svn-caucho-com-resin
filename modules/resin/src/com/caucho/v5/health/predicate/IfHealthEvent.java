/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import java.util.regex.Pattern;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.ScheduledCheckHealthEvent;

/**
 * Qualifies an action to match only on a health event.
 * <p>
 * <pre>{@code
 * <health:ThreadDump>
 *   <health:IfHealthEvent regexp="caucho.thread"/>
 * </health:ThreadDump>
 * }</pre>
 *
 */
@Configurable
public class IfHealthEvent extends HealthPredicateBase
{
  private Pattern _regexp;
  
  public IfHealthEvent()
  {
  }
  
  public IfHealthEvent(Pattern regexp)
  {
    setRegexp(regexp);
  }
  
  public IfHealthEvent(String regexp)
  {
    this(Pattern.compile(regexp));
  }
  
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }
  
  @Override
  public boolean isMatch(HealthEvent event)
  {
    if (! super.isMatch(event))
      return false;
    
    if (event instanceof ScheduledCheckHealthEvent)
      return false;
    
    if (_regexp == null)
      return true;
    
    return _regexp.matcher(event.getEventName()).find();
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.health.action.HealthAction;
import com.caucho.v5.jmx.server.*;
import com.caucho.v5.management.server.HealthActionMXBean;

public class HealthActionAdmin extends ManagedObjectBase
  implements HealthActionMXBean
{
  private final HealthAction _action;
  private final String _name;

  HealthActionAdmin(HealthAction action)
  {
    _action = action;
    // TODO: handle duplicates!
    _name = "Resin|" + action.getClass().getSimpleName();
  }
  
  public HealthAction getAction()
  {
    return _action;
  }
  
  public String getName()
  {
    return _name;
  }

  //
  // lifecycle
  //

  void register()
  {
    registerSelf();
  }
  
  void unregister()
  {
    unregisterSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}

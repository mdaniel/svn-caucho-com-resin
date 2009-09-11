/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.timer;

import javax.enterprise.inject.spi.Producer;
import javax.ejb.Timer;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import com.caucho.config.ConfigException;
import com.caucho.config.timer.TimeoutInvoker;
import com.caucho.util.L10N;

/**
 * Callback for a timer
 */
public class BeanTimeoutInvoker extends TimeoutInvoker {
  private static final L10N L = new L10N(BeanTimeoutInvoker.class);
  
  private Producer _producer;
  private Method _method;
  private boolean _isTimer;

  BeanTimeoutInvoker(Producer producer,
                     Method method)
  {
    _producer = producer;
    _method = method;

    Class []param = method.getParameterTypes();

    if (param.length == 0) {
    }
    else if (param.length == 1 && param[0].isAssignableFrom(Timer.class))
      _isTimer = true;
    else
      throw new ConfigException(L.l("'{0}' is an invalid schedule method",
                                    method));
  }
  
  public void timeout(Timer timer)
  {
    try {
      Object value = _producer.produce(null);

      _method.invoke(value);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}

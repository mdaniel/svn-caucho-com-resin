/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.profile;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Profile
{
  private static final L10N L = new L10N(Profile.class);
  private static final Logger log
    = Logger.getLogger(Profile.class.getName());
  
  protected Profile()
  {
  }

  public static Profile createProfile()
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> profileClass
        = Class.forName("com.caucho.profile.ProProfile", false, loader);

      Profile profile = (Profile) profileClass.newInstance();

      return profile;
    } catch (ClassNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);

      throw new ConfigException(L.l("Profile requires Resin Professional"));
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  public boolean isActive()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public int getDepth()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setDepth(int depth)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void start()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void stop()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getPeriod()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setPeriod(long period)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getTicks()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getRunTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public long getEndTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public long getGcTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public ProfileEntry []getResults()
  {
    throw new AbstractMethodError();
  }
}

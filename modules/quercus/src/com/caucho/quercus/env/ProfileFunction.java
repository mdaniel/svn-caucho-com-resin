/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.env;

import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a compiled function
 */
public class ProfileFunction extends AbstractFunction {
  private static final Logger log
    = Logger.getLogger(ProfileFunction.class.getName());
  private static final L10N L = new L10N(ProfileFunction.class);

  private final AbstractFunction _fun;
  private final int _id;
  
  public ProfileFunction(AbstractFunction fun, int id)
  {
    _fun = fun;
    _id = id;
  }

  public String getName()
  {
    return _fun.getName();
  }
  
  /*
   * Returns the name of the implementing class.
   */
  public String getDeclaringClassName()
  {
    return _fun.getDeclaringClassName();
  }

  public Value call(Env env, Value []args)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, args);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env,
		    Value a1)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, a1);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env,
		    Value a1,
		    Value a2)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, a1, a2);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env,
		    Value a1,
		    Value a2,
		    Value a3)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, a1, a2, a3);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env,
		    Value a1,
		    Value a2,
		    Value a3,
		    Value a4)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, a1, a2, a3, a4);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value call(Env env,
		    Value a1,
		    Value a2,
		    Value a3,
		    Value a4,
		    Value a5)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.call(env, a1, a2, a3, a4, a5);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env, Value []args)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, args);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env,
		       Value a1)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, a1);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env,
		       Value a1,
		       Value a2)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, a1, a2);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env,
		       Value a1,
		       Value a2,
		       Value a3)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, a1, a2, a3);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env,
		       Value a1,
		       Value a2,
		       Value a3,
		       Value a4)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, a1, a2, a3, a4);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public Value callRef(Env env,
		       Value a1,
		       Value a2,
		       Value a3,
		       Value a4,
		       Value a5)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _fun.callRef(env, a1, a2, a3, a4, a5);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "," + _id + "]";
  }
}


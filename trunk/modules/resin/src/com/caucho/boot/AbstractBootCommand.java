/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import com.caucho.util.L10N;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractBootCommand implements BootCommand {
  private static L10N _L;
  
  private HashSet<String> _optionSet = new HashSet<String>();

  @Override
  public String getName()
  {
    return "abstract-boot-command";
  }

  public void validateArgs(String[] args) throws BootArgumentException
  {
    Set<String> intValueKeys = getIntValueKeys();

    for (int i = 0; i < args.length; i++) {
      final String arg = args[i];

      if (getName().equals(arg)) {
        continue;
      }

      if (arg.startsWith("-J")
          || arg.startsWith("-D")
          || arg.startsWith("-X")) {
        continue;
      }

      if (arg.equals("-d64") || arg.equals("-d32")) {
        continue;
      }

      if (isOptionArg(arg))
        continue;

      if (! isValueArg(arg))
        throw new BootArgumentException(L().l("unknown argument '{0}'", arg));

      if (i + 1 == args.length)
        throw new BootArgumentException(L().l("option '{0}' requires a value",
                                              arg));
      String value = args[++i];

      if (isValueArg(value) || isOptionArg(value))
        throw new BootArgumentException(L().l("option '{0}' requires a value",
                                              arg));

      if (intValueKeys.contains(arg)) {
        try {
          Long.parseLong(value);
        } catch (NumberFormatException e) {
          throw new BootArgumentException(L().l("'{0}' argument must be a number: `{1}'", arg, value));
        }
      }
    }
  }
  
  protected boolean isOptionArg(String arg)
  {
    return getOptions().contains(arg);
  }
  
  protected boolean isValueArg(String arg)
  {
    return getValueKeys().contains(arg);
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(AbstractStartCommand.class);

    return _L;
  }

  @Override
  public Set<String> getOptions()
  {
    return _optionSet;
  }

  @Override
  public Set<String> getValueKeys()
  {
    return new HashSet<String>();
  }

  @Override
  public Set<String> getIntValueKeys()
  {
    return new HashSet<String>();
  }

  @Override
  public boolean isRetry()
  {
    return false;
  }

  @Override
  public void usage() {
  }
}

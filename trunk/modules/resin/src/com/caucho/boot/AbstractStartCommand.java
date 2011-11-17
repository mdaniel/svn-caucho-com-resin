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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.util.L10N;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractStartCommand extends AbstractBootCommand
{
  private static Logger _log;
  private static L10N _L;

  private final Set<String> _options = new HashSet<String>();
  private final Set<String> _valueKeys = new HashSet<String>();
  private final Set<String> _intValueKeys = new HashSet<String>();

  protected AbstractStartCommand()
  {
    _options.add("-verbose");
    _options.add("--verbose");
    _options.add("-preview");
    _options.add("--preview");

    _valueKeys.add("-conf");
    _valueKeys.add("--conf");
    _valueKeys.add("-data-directory");
    _valueKeys.add("--data-directory");
    _valueKeys.add("-join");
    _valueKeys.add("--join");
    _valueKeys.add("-join-cluster");
    _valueKeys.add("--join--cluster");
    _valueKeys.add("-log-directory");
    _valueKeys.add("--log-directory");
    _valueKeys.add("-resin-home");
    _valueKeys.add("--resin-home");
    _valueKeys.add("-root-directory");
    _valueKeys.add("--root-directory");
    _valueKeys.add("-server");
    _valueKeys.add("--server");
    _valueKeys.add("-stage");
    _valueKeys.add("--stage");
    _valueKeys.add("-watchdog-port");
    _valueKeys.add("--watchdog-port");
    _valueKeys.add("-debug-port");
    _valueKeys.add("--debug-port");
    _valueKeys.add("-jmx-port");
    _valueKeys.add("--jmx-port");

    _intValueKeys.add("-watchdog-port");
    _intValueKeys.add("--watchdog-port");
    _intValueKeys.add("-debug-port");
    _intValueKeys.add("--debug-port");
    _intValueKeys.add("-jmx-port");
    _intValueKeys.add("--jmx-port");
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(AbstractStartCommand.class.getName());

    return _log;
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
    return _options;
  }

  @Override
  public Set<String> getValueKeys()
  {
    return _valueKeys;
  }

  @Override
  public Set<String> getIntValueKeys()
  {
    return _intValueKeys;
  }
}

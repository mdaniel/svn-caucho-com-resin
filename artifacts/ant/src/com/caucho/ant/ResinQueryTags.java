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
 * @author Emil Ong
 */

package com.caucho.ant;

import org.apache.tools.ant.BuildException;

import java.util.List;

/**
 * Ant task to query tags of Resin applications deployed via the
 * ResinDeployWar task to production.
 */
public class ResinQueryTags extends ResinDeployClientTask {
  private String _pattern;
  private boolean _printValues = false;

  /**
   * For ant.
   **/
  public ResinQueryTags()
  {
  }

  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPrintValues(boolean printValues)
  {
    _printValues = printValues;
  }

  public boolean getPrintValues()
  {
    return _printValues;
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_pattern == null
        && getStage() == null
        && getHost() == null
        && getContext() == null
        && getVersion() == null)
      throw new BuildException("At least one of pattern, stage, virtualHost, contextRoot, or version is required by " + getTaskName());
  }

  @Override
  protected void fillArgs(List<String> args)
  {
    args.add("list");

    fillBaseArgs(args);

    if (_pattern != null) {
      args.add(_pattern);

      return;
    }

    StringBuilder pattern = new StringBuilder("^");
    if (_stage != null && ! _stage.isEmpty() && ! ".*".equals(_stage)) {
      pattern.append(_stage);
    } else {
      pattern.append("[^/]+");
    }

    pattern.append("/webapp/");

    if (_host != null && ! _host.isEmpty() && ! ".*".equals(_host)) {
      pattern.append(_host);
    } else {
      pattern.append("[^/]+");
    }

    boolean hasVersion = _version != null
      && !_version.isEmpty()
      && ! ".*".equals(_version);

    String version = null;

    if (hasVersion)
      version = _version.replace(".", "\\.");

    if (_context != null && ! _context.isEmpty() && ! ".*".equals(_context) && hasVersion) {
      pattern.append('/').append(_context).append("-").append(version).append(".*");
    } else if (_context != null && ! _context.isEmpty() && ! ".*".equals(_context)) {
      pattern.append('/').append(_context);
    } else if (hasVersion){
      pattern.append("/[^/]+-").append(version).append(".*");
    } else {
      pattern.append("/.*");
    }

    args.add(pattern.toString());
  }
}

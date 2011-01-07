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
 * Ant task to deploy war files to resin
 */
public class ResinUploadWar extends ResinDeployClientTask
{
  private String _warFile;

  /**
   * For ant.
   */
  public ResinUploadWar()
  {
  }

  public void setWarFile(String warFile)
    throws BuildException
  {
    if (!warFile.endsWith(".war"))
      throw new BuildException("war-file must have .war extension");

    _warFile = warFile;

    if (getContext() == null) {
      int lastSlash = _warFile.lastIndexOf("/");

      if (lastSlash < 0)
        lastSlash = 0;

      int end = _warFile.length() - ".war".length();
      String name = _warFile.substring(lastSlash, end);

      setContext(name);
    }
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_warFile == null)
      throw new BuildException("war-file is required by " + getTaskName());
  }

  @Override protected void fillArgs(List<String> args)
  {
    args.add("deploy");
    fillBaseArgs(args);

    if (getStage() != null) {
      args.add("-stage");
      args.add(getStage());
    }

    if (getVersion() != null) {
      args.add("-version");
      args.add(getVersion());
    }

    if (getHost() != null) {
      args.add("-host");
      args.add(getHost());
    }

    args.add("-name");
    args.add(getContext());

    args.add(_warFile);
  }
}

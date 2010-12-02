/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * Ant task to copy a tag in the repository.  A tag can be copied by
 * specifying the source tag explicitly using the "sourceTag" attribute
 * or using the "sourceStage", "sourceType", "sourceVirtualHost",
 * "sourceContextRoot", and "sourceVersion" attributes.  The target
 * tag can be specified explicitly using the "tag" attribute or by using
 * the "stage", "type", "virtualHost", "contextRoot", and "version"
 * attributes.
 */
public class ResinCopyTag extends ResinDeployClientTask
{
  private String _sourceContext;
  private String _sourceStage;
  private String _sourceVersion;
  private String _sourceHost;

  private String _targetStage;
  private String _targetVersion;
  private String _targetHost;
  private String _targetContext;

  /**
   * For ant.
   */
  public ResinCopyTag()
  {
  }

  public String getSourceContext()
  {
    return _sourceContext;
  }

  public void setSourceContext(String sourceContext)
  {
    _sourceContext = sourceContext;
  }

  public String getSourceStage()
  {
    return _sourceStage;
  }

  public void setSourceStage(String sourceStage)
  {
    _sourceStage = sourceStage;
  }

  public String getSourceVersion()
  {
    return _sourceVersion;
  }

  public void setSourceVersion(String sourceVersion)
  {
    _sourceVersion = sourceVersion;
  }

  public String getSourceHost()
  {
    return _sourceHost;
  }

  public void setSourceHost(String sourceHost)
  {
    _sourceHost = sourceHost;
  }

  public String getTargetStage()
  {
    return _targetStage;
  }

  public void setTargetStage(String targetStage)
  {
    _targetStage = targetStage;
  }

  public String getTargetVersion()
  {
    return _targetVersion;
  }

  public void setTargetVersion(String targetVersion)
  {
    _targetVersion = targetVersion;
  }

  public String getTargetHost()
  {
    return _targetHost;
  }

  public void setTargetHost(String targetHost)
  {
    _targetHost = targetHost;
  }

  public String getTargetContext()
  {
    return _targetContext;
  }

  public void setTargetContext(String targetContext)
  {
    _targetContext = targetContext;
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_sourceContext == null || _sourceContext.isEmpty())
      throw new BuildException("sourcesContext is required by "
        + getTaskName());

    if (_targetContext == null || _targetContext.isEmpty())
      throw new BuildException("sourceTag or sourceContextRoot is required by "
        + getTaskName());
  }

  @Override
  protected void fillArgs(List<String> args)
  {
    args.add("copy");

    fillBaseArgs(args);

    args.add("-source");
    args.add(_sourceContext);

    if(_sourceHost != null && ! _sourceHost.isEmpty()) {
      args.add("-source-host");
      args.add(_sourceHost);
    }

    if(_sourceStage != null && ! _sourceStage.isEmpty()) {
      args.add("-source-stage");
      args.add(_sourceStage);
    }

    if (_sourceVersion != null && ! _sourceVersion.isEmpty()) {
      args.add("-source-version");
      args.add(_sourceVersion);
    }

    args.add("-target");
    args.add(_targetContext);

    if(_targetHost != null && ! _targetHost.isEmpty()) {
      args.add("-target-host");
      args.add(_targetHost);
    }

    if(_targetStage != null && ! _targetStage.isEmpty()) {
      args.add("-target-stage");
      args.add(_targetStage);
    }

    if (_targetVersion != null && ! _targetVersion.isEmpty()) {
      args.add("-target-version");
      args.add(_targetVersion);
    }

  }
}

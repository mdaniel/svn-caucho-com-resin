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

package com.caucho.server.resin;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.util.L10N;

/**
 * The BootClusterConfig is the first-pass configuration of the cluster.
 * 
 * It matches the &lt;cluster> tag in the resin.xml
 */
public class BootClusterProxy implements SchemaBean
{
  private static final L10N L = new L10N(BootClusterProxy.class);
  
  private String _id;

  private ContainerProgram _program
    = new ContainerProgram();

  /*
   * A proxy for the cluster before merging.
   */
  public BootClusterProxy()
  {
  }

  /**
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }
  
  /**
   * Returns the cluster's id
   */
  public String getId()
  {
    return _id;
  }
  
  /**
   * Sets the cluster's id
   */
  @Configurable
  public void setId(String id)
  {
    _id = id;
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  ConfigProgram getProgram()
  {
    return _program;
  }
  
  @PostConstruct
  public void init()
  {
    if (_id == null)
      throw new ConfigException(L.l("'id' is a required attribute for <cluster>"));
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.server.deploy;

import com.caucho.json.JsonName;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.server.admin.ManagementQueryReply;

/**
 * This class is used with web-app-stop, web-app-start, web-app-restart,
 *
 */
@SuppressWarnings("serial")
public class DeployControllerState extends ManagementQueryReply
{
  @JsonName("state")
  private LifecycleState _state;

  @JsonName("tag")
  private String _tag;

  public DeployControllerState()
  {
  }

  public DeployControllerState(String tag, LifecycleState state)
  {
    _tag = tag;
    _state = state;
  }

  public LifecycleState getState()
  {
    return _state;
  }

  public void setState(LifecycleState state)
  {
    _state = state;
  }

  public String getTag()
  {
    return _tag;
  }

  public void setTag(String tag)
  {
    _tag = tag;
  }

  public String toString() {
    return this.getClass().getSimpleName() + "[" + _tag + ", " + _state + "]";
  }
}

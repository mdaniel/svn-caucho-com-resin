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
package com.caucho.server.admin;

import java.beans.ConstructorProperties;

import com.caucho.json.JsonName;

@SuppressWarnings("serial")
public class JmxCallQueryReply extends ManagementQueryReply
{
  @JsonName("bean")
  private String _bean;
  @JsonName("operation")
  private String _operation;
  @JsonName("return-value")
  private String _returnValue;

  public JmxCallQueryReply()
  {
  }

  @ConstructorProperties({"bean", "operation", "returnValue"})
  public JmxCallQueryReply(String bean,
                           String operation,
                           String returnValue)
  {
    _bean = bean;
    _operation = operation;
    _returnValue = returnValue;
  }

  public String getBean()
  {
    return _bean;
  }

  public String getOperation()
  {
    return _operation;
  }

  public String getReturnValue()
  {
    return _returnValue;
  }
}

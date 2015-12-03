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
package com.caucho.server.admin;

import com.caucho.json.Json;

@SuppressWarnings("serial")
public class JmxSetQueryReply extends ManagementQueryReply
{
  @Json(name = "bean")
  private String _bean;
  @Json(name = "attribute")
  private String _attribute;
  @Json(name = "old-value")
  private String _oldValue;
  @Json(name = "new-value")
  private String _newValue;

  public JmxSetQueryReply()
  {
  }

  public JmxSetQueryReply(String bean,
                          String attribute,
                          String oldValue,
                          String newValue)
  {
    _bean = bean;
    _attribute = attribute;
    _oldValue = oldValue;
    _newValue = newValue;
  }

  public String getBean()
  {
    return _bean;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  public String getOldValue()
  {
    return _oldValue;
  }

  public String getNewValue()
  {
    return _newValue;
  }
}

/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import javax.management.*;

public class JmxInfoResult implements java.io.Serializable
{
  private MBeanInfoHandler _info;

  private JmxInfoResult()
  {
  }

  public JmxInfoResult(MBeanInfo info)
  {
    _info = new MBeanInfoHandler(info);
  }

  public MBeanInfo getInfo()
  {
    return _info.toMBeanInfo();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _info + "]";
  }
}

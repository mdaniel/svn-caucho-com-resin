/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Sam
 */


package com.caucho.cloud.jmx;

import javax.management.MBeanInfo;
import java.util.HashMap;
import java.io.IOException;

public interface JmxServiceAPI
{
  public static final String SERVICE_NAME = "resin-jmx-service";

  public MBeanInfo getMBeanInfo(String name)
    throws IOException;

  public HashMap lookup(String name)
    throws IOException;

  public String []query(String pattern)
    throws IOException;

  public Object invoke(String objectName, String methodName,
                       Object []args, String []sig)
    throws IOException;
}

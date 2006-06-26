/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.quercus.lib.resin;

import com.caucho.quercus.env.Value;

import javax.management.Attribute;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MBean {
  private static final Logger log = Logger.getLogger(MBean.class.getName());
  
  private javax.management.MBeanServer _server;
  private ObjectName _name;
  private MBeanInfo _info;
  
  MBean(javax.management.MBeanServer server, ObjectName name)
  {
    _server = server;
    _name = name;
  }

  public String getMbeanName()
  {
    return _name.toString();
  }

  public MBeanInfo getInfo()
  {
    try {
      if (_info == null)
        _info = _server.getMBeanInfo(_name);

      return _info;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns an attribute.
   */
  public Object __getField(String attrName)
  {
    try {
      return unmarshall(_server.getAttribute(_name, attrName));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Sets an attribute.
   */
  public boolean __setField(String attrName, Object value)
  {
    try {
      _server.setAttribute(_name, new Attribute(attrName, value));

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    }
  }

  /**
   * Calls a method.
   */
  public Object __call(String name, Value []values)
  {
    try {
      Object []args = new Object[values.length];
      String []sig = new String[values.length];

      for (int i = 0; i < values.length; i++) {
	args[i] = values[i].toJavaObject();

	if (args[i] != null)
	  sig[i] = args[i].getClass().getName();
	else
	  sig[i] = "java.lang.Object";
      }

      sig = findClosestOperation(name, sig);
      
      return unmarshall(_server.invoke(_name, name, args, sig));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  private String []findClosestOperation(String name, String []sig)
    throws Exception
  {
    MBeanInfo info = getInfo();

    MBeanOperationInfo []ops = info.getOperations();

    for (int i = 0; i < ops.length; i++) {
      MBeanOperationInfo op = ops[i];
      
      if (! name.equals(op.getName()))
	continue;

      if (op.getSignature().length == sig.length) {
	sig = new String[sig.length];

	MBeanParameterInfo []params = op.getSignature();

	for (int j = 0; j < params.length; j++) {
	  sig[j] = params[j].getType();
	}

	return sig;
      }
    }

    return null;
  }

  private Object unmarshall(Object value)
  {
    if (value instanceof ObjectName) {
      ObjectName name = (ObjectName) value;

      return new MBean(_server, name);
    }
    else if (value instanceof ObjectName[]) {
      ObjectName []names = (ObjectName []) value;

      MBean []mbeans = new MBean[names.length];

      for (int i = 0; i < names.length; i++)
	mbeans[i] = new MBean(_server, names[i]);

      return mbeans;
    }
    else
      return value;
  }

  public String toString()
  {
    return "MBean[" + _name + "]";
  }
}

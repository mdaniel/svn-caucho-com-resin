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
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MBean {
  private static final Logger log = Logger.getLogger(MBean.class.getName());

  private static final HashMap<String,Marshall> _marshallMap
    = new HashMap<String,Marshall>();

  private MBeanServerConnection _server;
  private ObjectName _name;
  private MBeanInfo _info;

  MBean(MBeanServerConnection server, ObjectName name)
  {
    _server = server;
    _name = name;
  }

  /**
   * Returns the mbean's canonical name.
   */
  public String getMbean_name()
  {
    return _name.getCanonicalName();
  }

  /**
   * Returns the MBeanInfo for the mbean.
   */
  public MBeanInfo getMbean_info()
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

      String []mbeanSig = findClosestOperation(name, sig);

      if (mbeanSig != null) {
	marshall(args, mbeanSig);
	
	return unmarshall(_server.invoke(_name, name, args, mbeanSig));
      }
      else
	return null;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  protected String []findClosestOperation(String name, String []sig)
    throws Exception
  {
    MBeanInfo info = getMbean_info();

    MBeanOperationInfo []ops = info.getOperations();
    
    MBeanOperationInfo bestOp = null;
    long bestCost = Long.MAX_VALUE;

    for (int i = 0; i < ops.length; i++) {
      MBeanOperationInfo op = ops[i];
      
      if (! name.equals(op.getName()))
	continue;

      if (op.getSignature().length == sig.length) {
	long cost = calculateCost(op.getSignature(), sig);

	if (cost < bestCost) {
	  bestCost = cost;
	  bestOp = op;
	}
      }
    }

    if (bestOp != null) {
      String []mbeanSig = new String[sig.length];

      MBeanParameterInfo []params = bestOp.getSignature();

      for (int j = 0; j < params.length; j++) {
	mbeanSig[j] = params[j].getType();
      }

      return mbeanSig;
    }
      
    return null;
  }

  private static long calculateCost(MBeanParameterInfo []paramInfo,
				    String []args)
  {
    long cost = 0;
    
    for (int i = 0; i < paramInfo.length; i++) {
      String param = paramInfo[i].getType();
      String arg = args[i];
      
      if (param.equals(arg)) {
      }
      else if ((param.indexOf('[') >= 0) != (arg.indexOf('[') >= 0)) {
	cost += 100;
      }
      else
	cost += 1;
    }

    return cost;
  }

  private void marshall(Object []args, String []sig)
  {
    for (int i = 0; i < sig.length; i++) {
      args[i] = findMarshall(sig[i]).marshall(args[i]);
    }
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

  private Marshall findMarshall(String sig)
  {
    Marshall marshall = _marshallMap.get(sig);

    if (marshall != null)
      return marshall;
    else
      return Marshall.MARSHALL;
  }

  public String toString()
  {
    if (_name == null)
      return "MBean[]";
    else
      return "MBean[" + _name.getCanonicalName() + "]";
  }

  static class Marshall {
    static final Marshall MARSHALL = new Marshall();
    
    public Object marshall(Object value)
    {
      return value;
    }
  }

  static class IntMarshall extends Marshall {
    static final Marshall MARSHALL = new IntMarshall();
    
    public Object marshall(Object value)
    {
      if (value instanceof Integer)
	return value;
      else if (value instanceof Number)
	return new Integer(((Number) value).intValue());
      else if (value == null)
	return new Integer(0);
      else {
	try {
	  return new Integer(Integer.parseInt(String.valueOf(value)));
	} catch (Exception e) {
	  return new Integer(0);
	}
      }
    }
  }

  static class LongMarshall extends Marshall {
    static final Marshall MARSHALL = new LongMarshall();
    
    public Object marshall(Object value)
    {
      if (value instanceof Long)
	return value;
      else if (value instanceof Number)
	return new Long(((Number) value).longValue());
      else if (value == null)
	return new Long(0);
      else {
	try {
	  return new Long(Long.parseLong(String.valueOf(value)));
	} catch (Exception e) {
	  return new Long(0);
	}
      }
    }
  }

  static class StringMarshall extends Marshall {
    static final Marshall MARSHALL = new StringMarshall();
    
    public Object marshall(Object value)
    {
      if (value == null)
	return null;
      else
	return value.toString();
    }
  }

  static {
    _marshallMap.put("int", IntMarshall.MARSHALL);
    _marshallMap.put("java.lang.Integer", IntMarshall.MARSHALL);
    
    _marshallMap.put("long", LongMarshall.MARSHALL);
    _marshallMap.put("java.lang.Long", LongMarshall.MARSHALL);
    
    _marshallMap.put("java.lang.String", StringMarshall.MARSHALL);
  }
}

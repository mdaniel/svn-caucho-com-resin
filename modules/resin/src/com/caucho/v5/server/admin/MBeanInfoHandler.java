/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.admin;

import java.io.Serializable;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

/**
 * Adapter for MBeanInfo
 */
@SuppressWarnings("serial")
public class MBeanInfoHandler implements Serializable
{
  private String _className;
  private String _description;

  private MBeanAttributeInfoHandle []_attr;
  private MBeanOperationInfoHandle []_ops;
  
  public MBeanInfoHandler(MBeanInfo info)
  {
    _className = info.getClassName();
    _description = info.getDescription();

    MBeanAttributeInfo []attrs = info.getAttributes();
    _attr = new MBeanAttributeInfoHandle[attrs.length];

    for (int i = 0; i < attrs.length; i++) {
      _attr[i] = new MBeanAttributeInfoHandle(attrs[i]);
    }

    MBeanOperationInfo []ops = info.getOperations();
    _ops = new MBeanOperationInfoHandle[ops.length];

    for (int i = 0; i < ops.length; i++) {
      _ops[i] = new MBeanOperationInfoHandle(ops[i]);
    }
  }
  
  public MBeanInfoHandler()
  {
  }

  public MBeanInfo toMBeanInfo()
  {
    MBeanAttributeInfo []attrs = new MBeanAttributeInfo[_attr.length];

    for (int i = 0; i < attrs.length; i++) {
      attrs[i] = _attr[i].toInfo();
    }
    
    MBeanOperationInfo []ops = new MBeanOperationInfo[_ops.length];

    for (int i = 0; i < ops.length; i++) {
      ops[i] = _ops[i].toInfo();
    }
    
    return new MBeanInfo(_className,
			 _description,
			 attrs,
			 new MBeanConstructorInfo[0],
			 ops,
			 new MBeanNotificationInfo[0]);
  }

  static class MBeanAttributeInfoHandle implements Serializable {
    private String _name;
    private String _type;
    private String _description;
    private boolean _isReadable;
    private boolean _isWritable;
    private boolean _isIs;
    
    MBeanAttributeInfoHandle()
    {
    }
    
    MBeanAttributeInfoHandle(MBeanAttributeInfo attrInfo)
    {
      _name = attrInfo.getName();
      _type = attrInfo.getType();
      _description = attrInfo.getDescription();
      
      _isIs = attrInfo.isIs();
      _isReadable = attrInfo.isReadable();
      _isWritable = attrInfo.isWritable();
    }

    MBeanAttributeInfo toInfo()
    {
      return new MBeanAttributeInfo(_name,
				    _type,
				    _description,
				    _isReadable,
				    _isWritable,
				    _isIs);
    }
  }

  static class MBeanOperationInfoHandle implements Serializable {
    private String _name;
    private String _description;
    private MBeanParameterInfoHandle []_sig;
    private String _type;
    private int _impact;
    
    MBeanOperationInfoHandle()
    {
    }
    
    MBeanOperationInfoHandle(MBeanOperationInfo opInfo)
    {
      _name = opInfo.getName();
      _description = opInfo.getDescription();
      _type = opInfo.getReturnType();
      _impact = opInfo.getImpact();

      MBeanParameterInfo []sig = opInfo.getSignature();

      _sig = new MBeanParameterInfoHandle[sig.length];
      for (int i = 0; i < sig.length; i++)
	_sig[i] = new MBeanParameterInfoHandle(sig[i]);
    }

    MBeanOperationInfo toInfo()
    {
      MBeanParameterInfo []sig = new MBeanParameterInfo[_sig.length];

      for (int i = 0; i < sig.length; i++) {
	sig[i] = _sig[i].toInfo();
      }
      
      return new MBeanOperationInfo(_name,
				    _description,
				    sig,
				    _type,
				    _impact);
    }
    
  }

  static class MBeanParameterInfoHandle implements Serializable {
    private String _name;
    private String _description;
    private String _type;
    
    MBeanParameterInfoHandle()
    {
    }
    
    MBeanParameterInfoHandle(MBeanParameterInfo paramInfo)
    {
      _name = paramInfo.getName();
      _description = paramInfo.getDescription();
      _type = paramInfo.getType();
    }

    MBeanParameterInfo toInfo()
    {
      return new MBeanParameterInfo(_name, _type, _description);
    }
  }
}

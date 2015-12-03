/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Password implements PasswordApi {
  private static final Logger log
    = Logger.getLogger(Password.class.getName());
  
  private Password _passwordImpl;
  private String _value;
    
  public Password()
  {
    try {
      Class<?> cl = Class.forName("com.caucho.admin.PasswordImpl");
      
      _passwordImpl = (Password) cl.newInstance();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
  }
  
  protected Password(boolean isChild)
  {
  }
  
  public void setSalt(String salt)
  {
    if (_passwordImpl != null) {
      _passwordImpl.setSalt(salt);
    }
  }
  
  public void setValue(String value)
  {
    if (_passwordImpl != null) {
      _passwordImpl.setValue(value);
    }
    
    _value = value;
  }
  
  public Object replaceObject()
  {
    if (_passwordImpl != null) {
      return _passwordImpl.replaceObject();
    }
    else {
      return _value;
    }
  }

  @Override
  public String encrypt(String value, String salt)
    throws Exception
  {
    if (_passwordImpl != null) {
      return _passwordImpl.encrypt(value,  salt);
    }
    else {
      return null;
    }
  }
}

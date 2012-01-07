/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

public interface PasswordApi {
  public String encrypt(String value, String salt)
    throws Exception;
}

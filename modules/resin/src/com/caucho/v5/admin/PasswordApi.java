/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

public interface PasswordApi {
  public String encrypt(String value, String salt)
    throws Exception;
}